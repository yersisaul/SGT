package cfbd.co.sgt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CMS web E6: documentos (brochures). Solo PDF con su propio límite de
 * tamaño, que calcula el servidor; sin archivo no se publica; el sitio
 * descarga solo lo publicado con el nombre original.
 */
@SpringBootTest(properties = {
        "app.rate-limit.requests-per-minute=5000",
        "app.rate-limit.auth-requests-per-minute=1000",
        "app.rate-limit.public-requests-per-minute=5000",
        "app.storage.web-document-max-size-mb=20"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebDocumentosIntegrationTest {

    private static final String DOCUMENTOS = "/api/web/documentos";
    private static final String PRODUCTOS = "/api/web/productos";
    private static final String PUBLICO = "/api/public/web";
    private static final String NOMBRE_PDF = "CFBD_BROCHURE AZOR ANALYTICS.pdf";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.default-password}")
    private String seedPassword;

    private String tokenAdministrador;
    private String tokenCliente;
    private final List<String> creados = new ArrayList<>();

    @BeforeAll
    void login() throws Exception {
        tokenAdministrador = login("administrador1@cfbd.co");
        tokenCliente = login("cliente1@cfbd.co");
    }

    @Test
    void losBrochuresSembradosEsperanSuPdf() throws Exception {
        JsonNode documentos = json(perform(get(DOCUMENTOS), tokenAdministrador, null, status().isOk()));
        JsonNode brochure = buscar(documentos, "titulo", "Brochure Azor Analytics");
        assertThat(brochure).isNotNull();
        assertThat(brochure.get("tipo").asText()).isEqualTo("BROCHURE");
        assertThat(brochure.get("nombre_producto").asText()).isEqualTo("Azor Analytics");
        assertThat(buscar(documentos, "titulo", "Brochure Postes de Atención Rápida")).isNotNull();
    }

    @Test
    void cicloCompletoDeUnDocumento() throws Exception {
        String idProducto = registrar(PRODUCTOS, json(perform(post(PRODUCTOS), tokenAdministrador,
                Map.of("nombre", "Producto doc " + UUID.randomUUID(), "publicado", true), status().isCreated()))
                .get("id_producto").asText());
        String slugProducto = json(perform(get(PRODUCTOS + "/" + idProducto), tokenAdministrador, null,
                status().isOk())).get("slug").asText();

        Map<String, Object> body = new HashMap<>();
        body.put("titulo", "Brochure prueba " + UUID.randomUUID());
        body.put("id_producto", idProducto);
        // Sin PDF no se puede publicar, ni al crear ni después.
        body.put("publicado", true);
        perform(post(DOCUMENTOS), tokenAdministrador, body, status().isConflict());
        body.remove("publicado");
        JsonNode creado = json(perform(post(DOCUMENTOS), tokenAdministrador, body, status().isCreated()));
        String id = registrar(DOCUMENTOS, creado.get("id_documento").asText());
        assertThat(creado.get("tipo").asText()).isEqualTo("BROCHURE");
        assertThat(creado.get("url_archivo").isNull()).isTrue();
        perform(patch(DOCUMENTOS + "/" + id + "/publicacion"), tokenAdministrador, Map.of("publicado", true),
                status().isConflict());

        // Solo PDF; el tamaño lo calcula el servidor y el nombre se limpia de rutas.
        subir(id, new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2}), status().isBadRequest());
        byte[] pdf = "%PDF-1.4 contenido de prueba".getBytes(StandardCharsets.UTF_8);
        JsonNode conPdf = json(subir(id, new MockMultipartFile("file", "C:\\brochures\\" + NOMBRE_PDF,
                "application/pdf", pdf), status().isOk()));
        assertThat(conPdf.get("tamano_bytes").asLong()).isEqualTo(pdf.length);
        assertThat(conPdf.get("nombre_original").asText()).isEqualTo(NOMBRE_PDF);
        assertThat(conPdf.get("url_archivo").asText()).startsWith(DOCUMENTOS + "/" + id + "/archivo?v=");

        // No publicado: invisible para el sitio.
        mockMvc.perform(get(PUBLICO + "/archivos/documentos/" + id)).andExpect(status().isNotFound());
        perform(patch(DOCUMENTOS + "/" + id + "/publicacion"), tokenAdministrador, Map.of("publicado", true),
                status().isOk());

        JsonNode delProducto = json(mockMvc.perform(get(PUBLICO + "/documentos?producto=" + slugProducto))
                .andExpect(status().isOk()).andReturn());
        assertThat(delProducto.size()).isEqualTo(1);
        JsonNode publico = delProducto.get(0);
        assertThat(publico.get("tamano_bytes").asLong()).isEqualTo(pdf.length);
        assertThat(publico.get("producto").get("slug").asText()).isEqualTo(slugProducto);
        assertThat(publico.has("id_documento")).isFalse();
        assertThat(json(mockMvc.perform(get(PUBLICO + "/documentos?producto=" + slugProducto + "&tipo=FICHA_TECNICA"))
                .andReturn()).size()).isZero();

        MvcResult descarga = mockMvc.perform(get(publico.get("url_archivo").asText()))
                .andExpect(status().isOk()).andReturn();
        assertThat(descarga.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(descarga.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("inline").contains("CFBD_BROCHURE");
        assertThat(descarga.getResponse().getContentAsByteArray()).isEqualTo(pdf);

        // El producto con documentos no se puede borrar.
        perform(delete(PRODUCTOS + "/" + idProducto), tokenAdministrador, null, status().isConflict());

        // Reemplazar el PDF recalcula el tamaño.
        byte[] otro = "%PDF-1.7 version nueva, mas larga".getBytes(StandardCharsets.UTF_8);
        JsonNode reemplazado = json(subir(id, new MockMultipartFile("file", "nuevo.pdf", "application/pdf", otro),
                status().isOk()));
        assertThat(reemplazado.get("tamano_bytes").asLong()).isEqualTo(otro.length);

        perform(patch(DOCUMENTOS + "/" + id + "/publicacion"), tokenAdministrador, Map.of("publicado", false),
                status().isOk());
        mockMvc.perform(get(PUBLICO + "/archivos/documentos/" + id)).andExpect(status().isNotFound());
        perform(get(DOCUMENTOS + "/" + id + "/archivo"), tokenAdministrador, null, status().isOk());
    }

    @Test
    void rechazaPdfMayorAlLimite() throws Exception {
        String id = registrar(DOCUMENTOS, json(perform(post(DOCUMENTOS), tokenAdministrador,
                Map.of("titulo", "Pesado " + UUID.randomUUID()), status().isCreated())).get("id_documento").asText());
        byte[] veintiunMb = new byte[21 * 1024 * 1024];
        subir(id, new MockMultipartFile("file", "pesado.pdf", "application/pdf", veintiunMb), status().isBadRequest());
    }

    @Test
    void elPanelExigeLosPermisosWebDocumento() throws Exception {
        mockMvc.perform(get(DOCUMENTOS)).andExpect(status().isUnauthorized());
        perform(get(DOCUMENTOS), tokenCliente, null, status().isForbidden());
        perform(post(DOCUMENTOS), tokenCliente, Map.of("titulo", "No"), status().isForbidden());
    }

    @AfterAll
    void eliminarCreados() throws Exception {
        // Orden inverso: primero los documentos, que referencian productos.
        List<String> rutas = new ArrayList<>(creados);
        Collections.reverse(rutas);
        for (String ruta : rutas) {
            mockMvc.perform(delete(ruta).header("Authorization", "Bearer " + tokenAdministrador));
        }
    }

    // ---------- Helpers ----------

    private MvcResult subir(String id, MockMultipartFile archivo, ResultMatcher esperado) throws Exception {
        return mockMvc.perform(multipart(DOCUMENTOS + "/" + id + "/archivo").file(archivo)
                        .header("Authorization", "Bearer " + tokenAdministrador))
                .andExpect(esperado).andReturn();
    }

    private String registrar(String base, String id) {
        creados.add(base + "/" + id);
        return id;
    }

    private JsonNode buscar(JsonNode lista, String campo, String valor) {
        for (JsonNode item : lista) {
            if (item.get(campo).asText().equals(valor)) {
                return item;
            }
        }
        return null;
    }

    private MvcResult perform(MockHttpServletRequestBuilder builder, String token, Object body, ResultMatcher esperado)
            throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder).andExpect(esperado).andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", seedPassword))))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("accessToken").asText();
    }
}
