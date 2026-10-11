package cfbd.co.sgt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CMS web E4: partners, clientes de referencia y casos de éxito
 * (web_comercial.*), con sus vínculos y lo que expone el sitio público.
 */
@SpringBootTest(properties = {
        "app.rate-limit.requests-per-minute=5000",
        "app.rate-limit.auth-requests-per-minute=1000",
        "app.rate-limit.public-requests-per-minute=5000"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebComercialIntegrationTest {

    private static final String PARTNERS = "/api/web/partners";
    private static final String CLIENTES = "/api/web/clientes";
    private static final String CASOS = "/api/web/casos-exito";
    private static final String PRODUCTOS = "/api/web/productos";
    private static final String PUBLICO = "/api/public/web";

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
    void elContenidoComercialSembradoSeVeEnElSitio() throws Exception {
        JsonNode partners = publico("/partners");
        JsonNode nx = buscar(partners, "nombre", "Network Optix");
        assertThat(nx.get("tipo").asText()).isEqualTo("TECNOLOGICO");
        assertThat(nx.get("url_sitio").asText()).isEqualTo("https://nxvms.com/integrations/471-azor");
        assertThat(nx.has("publicado")).isFalse();

        JsonNode marcas = publico("/partners?tipo=MARCA_COMPATIBLE");
        assertThat(buscar(marcas, "nombre", "Hikvision")).isNotNull();
        assertThat(buscar(marcas, "nombre", "Network Optix")).isNull();

        assertThat(buscar(publico("/clientes"), "nombre", "Backus")).isNotNull();

        JsonNode alertaSurco = publico("/casos-exito/alerta-surco");
        assertThat(alertaSurco.get("cliente").get("nombre").asText()).isEqualTo("Municipalidad de Santiago de Surco");
        assertThat(alertaSurco.get("producto").get("slug").asText()).isEqualTo("app-reporter");
        assertThat(alertaSurco.get("metricas").get(0).get("valor").asText()).isEqualTo("50 000");
        assertThat(alertaSurco.has("id_caso_exito")).isFalse();
    }

    @Test
    void casoDeExitoConVinculosQueProtegenSuBorrado() throws Exception {
        String sufijo = UUID.randomUUID().toString();
        String idCliente = crear(CLIENTES, Map.of("nombre", "Cliente " + sufijo, "sector", "Retail",
                "publicado", true), "id_cliente_referencia");
        String idProducto = crear(PRODUCTOS, Map.of("nombre", "Producto " + sufijo, "publicado", true), "id_producto");

        Map<String, Object> caso = new HashMap<>();
        caso.put("titulo", "Caso " + sufijo);
        caso.put("fecha_caso", "2025-03-15");
        caso.put("metricas", List.of(Map.of("valor", "30", "etiqueta", "postes instalados")));
        caso.put("id_cliente_referencia", idCliente);
        caso.put("id_producto", idProducto);
        caso.put("publicado", true);
        JsonNode creado = json(perform(post(CASOS), tokenAdministrador, caso, status().isCreated()));
        String idCaso = registrar(CASOS, creado.get("id_caso_exito").asText());
        String slug = creado.get("slug").asText();
        assertThat(creado.get("nombre_cliente").asText()).isEqualTo("Cliente " + sufijo);
        assertThat(creado.get("fecha_caso").asText()).isEqualTo("2025-03-15");

        JsonNode publico = publico("/casos-exito/" + slug);
        assertThat(publico.get("cliente").get("sector").asText()).isEqualTo("Retail");
        assertThat(publico.get("producto").get("nombre").asText()).isEqualTo("Producto " + sufijo);
        assertThat(publico.get("metricas").get(0).get("etiqueta").asText()).isEqualTo("postes instalados");

        // Un cliente despublicado no se filtra al sitio a través del caso.
        perform(patch(CLIENTES + "/" + idCliente + "/publicacion"), tokenAdministrador, Map.of("publicado", false),
                status().isOk());
        assertThat(publico("/casos-exito/" + slug).get("cliente").isNull()).isTrue();

        // Referenciados por el caso: no se pueden borrar.
        perform(delete(CLIENTES + "/" + idCliente), tokenAdministrador, null, status().isConflict());
        perform(delete(PRODUCTOS + "/" + idProducto), tokenAdministrador, null, status().isConflict());

        // Editar sin vínculos los libera (null = sin vínculo).
        caso.remove("id_cliente_referencia");
        caso.remove("id_producto");
        JsonNode editado = json(perform(put(CASOS + "/" + idCaso), tokenAdministrador, caso, status().isOk()));
        assertThat(editado.get("id_cliente_referencia").isNull()).isTrue();
        perform(delete(CLIENTES + "/" + idCliente), tokenAdministrador, null, status().isNoContent());

        caso.put("id_producto", UUID.randomUUID().toString());
        perform(put(CASOS + "/" + idCaso), tokenAdministrador, caso, status().isNotFound());
    }

    @Test
    void partnerConLogoYValidaciones() throws Exception {
        String id = crear(PARTNERS, Map.of("nombre", "Marca " + UUID.randomUUID(), "tipo", "MARCA_COMPATIBLE",
                "url_sitio", "https://ejemplo.com"), "id_partner");

        JsonNode conLogo = json(mockMvc.perform(multipart(PARTNERS + "/" + id + "/logo")
                        .file(new MockMultipartFile("file", "logo.webp", "image/webp", new byte[]{1, 2, 3}))
                        .header("Authorization", "Bearer " + tokenAdministrador))
                .andExpect(status().isOk()).andReturn());
        assertThat(conLogo.get("url_logo").asText()).startsWith(PARTNERS + "/" + id + "/logo?v=");
        assertThat(conLogo.get("tipo").asText()).isEqualTo("MARCA_COMPATIBLE");

        // No publicado: el logo no es público.
        mockMvc.perform(get(PUBLICO + "/archivos/partners/" + id)).andExpect(status().isNotFound());
        perform(patch(PARTNERS + "/" + id + "/publicacion"), tokenAdministrador, Map.of("publicado", true),
                status().isOk());
        mockMvc.perform(get(PUBLICO + "/archivos/partners/" + id)).andExpect(status().isOk());

        perform(post(PARTNERS), tokenAdministrador, Map.of("nombre", "X", "tipo", "OTRO"), status().isBadRequest());
        perform(post(PARTNERS), tokenAdministrador, Map.of("nombre", "X", "url_sitio", "ftp://x"),
                status().isBadRequest());
        perform(post(CASOS), tokenAdministrador, Map.of("titulo", "Caso", "metricas", List.of(Map.of("valor", "1"))),
                status().isBadRequest());
    }

    @Test
    void elPanelExigeLosPermisosWebComercial() throws Exception {
        for (String ruta : List.of(PARTNERS, CLIENTES, CASOS)) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            perform(get(ruta), tokenCliente, null, status().isForbidden());
            perform(get(ruta), tokenAdministrador, null, status().isOk());
        }
    }

    @AfterAll
    void eliminarCreados() throws Exception {
        // Orden inverso: primero los casos, que referencian a clientes y productos.
        List<String> rutas = new ArrayList<>(creados);
        Collections.reverse(rutas);
        for (String ruta : rutas) {
            mockMvc.perform(delete(ruta).header("Authorization", "Bearer " + tokenAdministrador));
        }
    }

    // ---------- Helpers ----------

    private String crear(String base, Map<String, Object> body, String campoId) throws Exception {
        JsonNode creado = json(perform(post(base), tokenAdministrador, body, status().isCreated()));
        return registrar(base, creado.get(campoId).asText());
    }

    private String registrar(String base, String id) {
        creados.add(base + "/" + id);
        return id;
    }

    private JsonNode publico(String ruta) throws Exception {
        return json(mockMvc.perform(get(PUBLICO + ruta)).andExpect(status().isOk()).andReturn());
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
