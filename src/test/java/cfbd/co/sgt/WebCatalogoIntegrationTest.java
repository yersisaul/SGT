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
 * CMS web E3: productos y servicios. CRUD del panel (web_catalogo.*), listas
 * JSONB, slugs, publicación, imágenes y API pública que solo expone lo publicado.
 */
@SpringBootTest(properties = {
        "app.rate-limit.requests-per-minute=5000",
        "app.rate-limit.auth-requests-per-minute=1000",
        "app.rate-limit.public-requests-per-minute=5000"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebCatalogoIntegrationTest {

    private static final String PRODUCTOS = "/api/web/productos";
    private static final String SERVICIOS = "/api/web/servicios";
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
    void elContenidoSembradoSeVeEnElSitioSinLogin() throws Exception {
        MvcResult result = mockMvc.perform(get(PUBLICO + "/productos")).andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("max-age=60").contains("public");

        JsonNode analytics = buscar(json(result), "slug", "azor-analytics");
        assertThat(analytics).isNotNull();
        assertThat(analytics.get("destacado").asBoolean()).isTrue();
        assertThat(analytics.get("modulos").size()).isGreaterThanOrEqualTo(20);
        assertThat(analytics.get("modulos").get(0).get("grupo").asText()).isEqualTo("Seguridad perimetral y protección");
        // La API pública no expone estado, auditoría ni ids internos.
        assertThat(analytics.has("publicado")).isFalse();
        assertThat(analytics.has("fecha_creacion")).isFalse();
        assertThat(analytics.has("id_producto")).isFalse();

        for (String slug : List.of("azor-panel", "app-reporter", "app-respondiente")) {
            mockMvc.perform(get(PUBLICO + "/productos/" + slug)).andExpect(status().isOk());
        }
        JsonNode servicios = json(mockMvc.perform(get(PUBLICO + "/servicios")).andExpect(status().isOk()).andReturn());
        assertThat(buscar(servicios, "slug", "configuracion-y-mantenimiento-de-vms")).isNotNull();
    }

    @Test
    void cicloCompletoDeUnProducto() throws Exception {
        String nombre = "Producto Prueba " + UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("nombre", nombre);
        body.put("descripcion_corta", "  Texto corto  ");
        body.put("caracteristicas", List.of("  Primera  ", "Segunda"));
        body.put("modulos", List.of(
                Map.of("nombre", "Cruce de línea", "grupo", "Seguridad perimetral", "descripcion", "Detecta cruces"),
                Map.of("nombre", "Conteo de personas")));
        JsonNode creado = json(perform(post(PRODUCTOS), tokenAdministrador, body, status().isCreated()));
        String id = registrar(PRODUCTOS, creado.get("id_producto").asText());
        String slug = creado.get("slug").asText();

        assertThat(slug).isEqualTo(SlugEsperado.de(nombre));
        assertThat(creado.get("publicado").asBoolean()).isFalse();
        assertThat(creado.get("descripcion_corta").asText()).isEqualTo("Texto corto");
        assertThat(creado.get("nombre_usuario_actualiza").asText()).isNotBlank();

        // Las listas JSONB vuelven intactas desde la BD.
        JsonNode leido = json(perform(get(PRODUCTOS + "/" + id), tokenAdministrador, null, status().isOk()));
        assertThat(leido.get("caracteristicas").get(0).asText()).isEqualTo("Primera");
        assertThat(leido.get("modulos").size()).isEqualTo(2);
        assertThat(leido.get("modulos").get(0).get("grupo").asText()).isEqualTo("Seguridad perimetral");
        assertThat(leido.get("modulos").get(1).get("grupo").isNull()).isTrue();

        // Mismo nombre → slug con sufijo; slug explícito repetido → 409.
        JsonNode segundo = json(perform(post(PRODUCTOS), tokenAdministrador, Map.of("nombre", nombre), status().isCreated()));
        registrar(PRODUCTOS, segundo.get("id_producto").asText());
        assertThat(segundo.get("slug").asText()).isEqualTo(slug + "-2");
        perform(post(PRODUCTOS), tokenAdministrador, Map.of("nombre", "Otro", "slug", slug), status().isConflict());

        // No publicado: invisible para el sitio.
        mockMvc.perform(get(PUBLICO + "/productos/" + slug)).andExpect(status().isNotFound());
        assertThat(buscar(json(mockMvc.perform(get(PUBLICO + "/productos")).andReturn()), "slug", slug)).isNull();

        perform(patch(PRODUCTOS + "/" + id + "/publicacion"), tokenAdministrador, Map.of("publicado", true),
                status().isOk());
        mockMvc.perform(get(PUBLICO + "/productos/" + slug)).andExpect(status().isOk());

        // PUT sin slug ni publicado conserva ambos.
        JsonNode editado = json(perform(put(PRODUCTOS + "/" + id), tokenAdministrador,
                Map.of("nombre", "Nombre editado"), status().isOk()));
        assertThat(editado.get("slug").asText()).isEqualTo(slug);
        assertThat(editado.get("publicado").asBoolean()).isTrue();
        assertThat(editado.get("caracteristicas").size()).isZero();

        // Imagen: subida, visible en el sitio solo mientras esté publicado.
        JsonNode conImagen = json(mockMvc.perform(multipart(PRODUCTOS + "/" + id + "/imagen")
                        .file(new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3, 4}))
                        .header("Authorization", "Bearer " + tokenAdministrador))
                .andExpect(status().isOk()).andReturn());
        assertThat(conImagen.get("url_img").asText()).startsWith(PRODUCTOS + "/" + id + "/imagen?v=");

        String publica = json(mockMvc.perform(get(PUBLICO + "/productos/" + slug)).andReturn()).get("url_img").asText();
        assertThat(publica).startsWith(PUBLICO + "/archivos/productos/" + id + "?v=");
        MvcResult imagen = mockMvc.perform(get(publica)).andExpect(status().isOk()).andReturn();
        assertThat(imagen.getResponse().getContentType()).isEqualTo("image/png");
        assertThat(imagen.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("max-age=604800");
        perform(get(PRODUCTOS + "/" + id + "/imagen"), tokenAdministrador, null, status().isOk());

        perform(patch(PRODUCTOS + "/" + id + "/publicacion"), tokenAdministrador, Map.of("publicado", false),
                status().isOk());
        mockMvc.perform(get(publica)).andExpect(status().isNotFound());
        mockMvc.perform(get(PUBLICO + "/archivos/otro-recurso/" + id)).andExpect(status().isNotFound());

        JsonNode sinImagen = json(perform(delete(PRODUCTOS + "/" + id + "/imagen"), tokenAdministrador, null,
                status().isOk()));
        assertThat(sinImagen.get("url_img").isNull()).isTrue();

        perform(delete(PRODUCTOS + "/" + id), tokenAdministrador, null, status().isNoContent());
        perform(get(PRODUCTOS + "/" + id), tokenAdministrador, null, status().isNotFound());
    }

    @Test
    void cicloDeUnServicioYRechazoDeSvg() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nombre", "Servicio Prueba " + UUID.randomUUID());
        body.put("icono", "server");
        body.put("publicado", true);
        JsonNode creado = json(perform(post(SERVICIOS), tokenAdministrador, body, status().isCreated()));
        String id = registrar(SERVICIOS, creado.get("id_servicio").asText());
        String slug = creado.get("slug").asText();

        JsonNode publico = json(mockMvc.perform(get(PUBLICO + "/servicios/" + slug)).andExpect(status().isOk()).andReturn());
        assertThat(publico.get("icono").asText()).isEqualTo("server");

        mockMvc.perform(multipart(SERVICIOS + "/" + id + "/imagen")
                        .file(new MockMultipartFile("file", "logo.svg", "image/svg+xml", "<svg/>".getBytes()))
                        .header("Authorization", "Bearer " + tokenAdministrador))
                .andExpect(status().isBadRequest());

        perform(delete(SERVICIOS + "/" + id), tokenAdministrador, null, status().isNoContent());
        mockMvc.perform(get(PUBLICO + "/servicios/" + slug)).andExpect(status().isNotFound());
    }

    @Test
    void validaLosDatosDelPanel() throws Exception {
        perform(post(PRODUCTOS), tokenAdministrador, Map.of("nombre", " "), status().isBadRequest());
        perform(post(PRODUCTOS), tokenAdministrador, Map.of("nombre", "¡¡!!"), status().isBadRequest());
        perform(post(PRODUCTOS), tokenAdministrador, Map.of("nombre", "Video inseguro", "url_video", "http://x.com"),
                status().isBadRequest());
        perform(post(PRODUCTOS), tokenAdministrador, Map.of("nombre", "Slug raro", "slug", "¡¡!!"),
                status().isBadRequest());
        perform(post(PRODUCTOS), tokenAdministrador, Map.of("nombre", "Módulo vacío", "modulos", List.of(Map.of())),
                status().isBadRequest());
        perform(post(SERVICIOS), tokenAdministrador, Map.of("nombre", "Icono raro", "icono", "Fa Server"),
                status().isBadRequest());
    }

    @Test
    void elPanelExigeLosPermisosWebCatalogo() throws Exception {
        mockMvc.perform(get(PRODUCTOS)).andExpect(status().isUnauthorized());
        perform(get(PRODUCTOS), tokenCliente, null, status().isForbidden());
        perform(post(PRODUCTOS), tokenCliente, Map.of("nombre", "No permitido"), status().isForbidden());
        perform(get(SERVICIOS), tokenCliente, null, status().isForbidden());
        perform(get(PRODUCTOS), tokenAdministrador, null, status().isOk());
    }

    @AfterAll
    void eliminarCreados() throws Exception {
        for (String ruta : creados) {
            mockMvc.perform(delete(ruta).header("Authorization", "Bearer " + tokenAdministrador));
        }
    }

    // ---------- Helpers ----------

    /** Slug esperado para los nombres de prueba (sin tildes ni símbolos). */
    private static final class SlugEsperado {
        static String de(String nombre) {
            return nombre.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
        }
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
