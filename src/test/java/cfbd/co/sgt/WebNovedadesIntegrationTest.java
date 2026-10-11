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
 * CMS web E5: novedades y categorías. Nacen en BORRADOR, se publican con
 * web_novedad.publish (también programadas o archivadas) y el sitio solo ve
 * las publicadas cuya fecha ya llegó.
 */
@SpringBootTest(properties = {
        "app.rate-limit.requests-per-minute=5000",
        "app.rate-limit.auth-requests-per-minute=1000",
        "app.rate-limit.public-requests-per-minute=5000"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebNovedadesIntegrationTest {

    private static final String NOVEDADES = "/api/web/novedades";
    private static final String CATEGORIAS = "/api/web/categorias-novedad";
    private static final String PUBLICO = "/api/public/web";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.default-password}")
    private String seedPassword;

    private String tokenAdministrador;
    private String tokenCliente;
    private String idCategoriaNoticias;
    private final List<String> creados = new ArrayList<>();

    @BeforeAll
    void login() throws Exception {
        tokenAdministrador = login("administrador1@cfbd.co");
        tokenCliente = login("cliente1@cfbd.co");
        for (JsonNode categoria : json(perform(get(CATEGORIAS), tokenAdministrador, null, status().isOk()))) {
            if (categoria.get("slug").asText().equals("noticias")) {
                idCategoriaNoticias = categoria.get("id_categoria_novedad").asText();
            }
        }
    }

    @Test
    void cicloDePublicacionProgramacionYArchivo() throws Exception {
        JsonNode creada = crearNovedad("Lanzamiento " + UUID.randomUUID(), "Primer párrafo.\n\nSegundo párrafo.");
        String id = creada.get("id_novedad").asText();
        String slug = creada.get("slug").asText();
        assertThat(creada.get("estado").asText()).isEqualTo("BORRADOR");
        assertThat(creada.get("nombre_autor").asText()).isNotBlank();
        assertThat(creada.get("nombre_categoria").asText()).isEqualTo("Noticias");

        // Borrador: invisible para el sitio.
        mockMvc.perform(get(PUBLICO + "/novedades/" + slug)).andExpect(status().isNotFound());

        JsonNode publicada = cambiarEstado(id, Map.of("estado", "PUBLICADO"), status().isOk());
        assertThat(publicada.get("visible_en_sitio").asBoolean()).isTrue();
        assertThat(publicada.get("fecha_publicacion").asText()).isNotBlank();

        MvcResult detalle = mockMvc.perform(get(PUBLICO + "/novedades/" + slug)).andExpect(status().isOk()).andReturn();
        JsonNode publico = json(detalle);
        assertThat(publico.get("contenido").asText()).isEqualTo("Primer párrafo.\n\nSegundo párrafo.");
        assertThat(publico.get("categoria").get("slug").asText()).isEqualTo("noticias");
        assertThat(publico.has("estado")).isFalse();
        assertThat(detalle.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("max-age=60");

        // En el listado (filtrado por categoría) aparece sin el contenido completo.
        JsonNode pagina = json(mockMvc.perform(get(PUBLICO + "/novedades?categoria=noticias&tamano=50"))
                .andExpect(status().isOk()).andReturn());
        JsonNode enLista = buscar(pagina.get("contenido"), slug);
        assertThat(enLista).isNotNull();
        assertThat(enLista.get("contenido").isNull()).isTrue();
        assertThat(buscar(json(mockMvc.perform(get(PUBLICO + "/novedades?categoria=eventos&tamano=50")).andReturn())
                .get("contenido"), slug)).isNull();

        // Portada visible en el sitio solo mientras la novedad lo esté.
        JsonNode conPortada = json(mockMvc.perform(multipart(NOVEDADES + "/" + id + "/imagen")
                        .file(new MockMultipartFile("file", "portada.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .header("Authorization", "Bearer " + tokenAdministrador))
                .andExpect(status().isOk()).andReturn());
        assertThat(conPortada.get("url_portada").asText()).startsWith(NOVEDADES + "/" + id + "/imagen?v=");
        mockMvc.perform(get(PUBLICO + "/archivos/novedades/" + id)).andExpect(status().isOk());

        // Programada (fecha futura): deja de verse.
        JsonNode programada = cambiarEstado(id, Map.of("estado", "PUBLICADO", "fecha_publicacion", "2099-01-01T00:00:00Z"),
                status().isOk());
        assertThat(programada.get("visible_en_sitio").asBoolean()).isFalse();
        mockMvc.perform(get(PUBLICO + "/novedades/" + slug)).andExpect(status().isNotFound());
        mockMvc.perform(get(PUBLICO + "/archivos/novedades/" + id)).andExpect(status().isNotFound());

        // Archivada: tampoco.
        cambiarEstado(id, Map.of("estado", "PUBLICADO", "fecha_publicacion", "2020-01-01T00:00:00Z"), status().isOk());
        mockMvc.perform(get(PUBLICO + "/novedades/" + slug)).andExpect(status().isOk());
        cambiarEstado(id, Map.of("estado", "ARCHIVADO"), status().isOk());
        mockMvc.perform(get(PUBLICO + "/novedades/" + slug)).andExpect(status().isNotFound());

        JsonNode archivadas = json(perform(get(NOVEDADES + "?estado=ARCHIVADO&tamano=50"), tokenAdministrador, null,
                status().isOk()));
        assertThat(buscar(archivadas.get("contenido"), slug)).isNotNull();
    }

    @Test
    void sinContenidoNoSePublica() throws Exception {
        JsonNode vacia = crearNovedad("Sin contenido " + UUID.randomUUID(), null);
        String id = vacia.get("id_novedad").asText();
        cambiarEstado(id, Map.of("estado", "PUBLICADO"), status().isConflict());

        JsonNode conTexto = crearNovedad("Con texto " + UUID.randomUUID(), "Texto");
        String idConTexto = conTexto.get("id_novedad").asText();
        cambiarEstado(idConTexto, Map.of("estado", "PUBLICADO"), status().isOk());
        // Publicada: no se le puede vaciar el contenido.
        perform(put(NOVEDADES + "/" + idConTexto), tokenAdministrador,
                Map.of("titulo", conTexto.get("titulo").asText()), status().isConflict());
        cambiarEstado(idConTexto, Map.of("estado", "nada"), status().isBadRequest());
    }

    @Test
    void paginacionDelSitio() throws Exception {
        JsonNode pagina = json(mockMvc.perform(get(PUBLICO + "/novedades?tamano=500")).andExpect(status().isOk())
                .andReturn());
        assertThat(pagina.get("tamano").asInt()).isEqualTo(50);
        assertThat(pagina.get("pagina").asInt()).isZero();
        assertThat(pagina.has("total_elementos")).isTrue();

        JsonNode uno = json(mockMvc.perform(get(PUBLICO + "/novedades?tamano=1&pagina=-3")).andReturn());
        assertThat(uno.get("contenido").size()).isLessThanOrEqualTo(1);
        assertThat(uno.get("pagina").asInt()).isZero();
    }

    @Test
    void categoriaEnUsoNoSeBorra() throws Exception {
        JsonNode categoria = json(perform(post(CATEGORIAS), tokenAdministrador,
                Map.of("nombre", "Categoría " + UUID.randomUUID()), status().isCreated()));
        String idCategoria = registrar(CATEGORIAS, categoria.get("id_categoria_novedad").asText());

        Map<String, Object> body = new HashMap<>();
        body.put("titulo", "En categoría " + UUID.randomUUID());
        body.put("id_categoria_novedad", idCategoria);
        registrar(NOVEDADES, json(perform(post(NOVEDADES), tokenAdministrador, body, status().isCreated()))
                .get("id_novedad").asText());

        perform(delete(CATEGORIAS + "/" + idCategoria), tokenAdministrador, null, status().isConflict());

        JsonNode publicas = json(mockMvc.perform(get(PUBLICO + "/categorias-novedad")).andExpect(status().isOk())
                .andReturn());
        assertThat(buscar(publicas, categoria.get("slug").asText())).isNotNull();
        assertThat(buscar(publicas, "noticias")).isNotNull();
    }

    @Test
    void elPanelExigeLosPermisosWebNovedad() throws Exception {
        mockMvc.perform(get(NOVEDADES)).andExpect(status().isUnauthorized());
        perform(get(NOVEDADES), tokenCliente, null, status().isForbidden());
        perform(get(CATEGORIAS), tokenCliente, null, status().isForbidden());
        perform(patch(NOVEDADES + "/" + UUID.randomUUID() + "/estado"), tokenCliente, Map.of("estado", "PUBLICADO"),
                status().isForbidden());
    }

    @AfterAll
    void eliminarCreados() throws Exception {
        // Orden inverso: primero las novedades, que referencian categorías.
        List<String> rutas = new ArrayList<>(creados);
        Collections.reverse(rutas);
        for (String ruta : rutas) {
            mockMvc.perform(delete(ruta).header("Authorization", "Bearer " + tokenAdministrador));
        }
    }

    // ---------- Helpers ----------

    private JsonNode crearNovedad(String titulo, String contenido) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("titulo", titulo);
        body.put("resumen", "Resumen de prueba");
        body.put("contenido", contenido);
        body.put("id_categoria_novedad", idCategoriaNoticias);
        JsonNode creada = json(perform(post(NOVEDADES), tokenAdministrador, body, status().isCreated()));
        registrar(NOVEDADES, creada.get("id_novedad").asText());
        return creada;
    }

    private JsonNode cambiarEstado(String id, Map<String, Object> body, ResultMatcher esperado) throws Exception {
        MvcResult result = perform(patch(NOVEDADES + "/" + id + "/estado"), tokenAdministrador, body, esperado);
        return result.getResponse().getStatus() == 200 ? json(result) : null;
    }

    private String registrar(String base, String id) {
        creados.add(base + "/" + id);
        return id;
    }

    private JsonNode buscar(JsonNode lista, String slug) {
        for (JsonNode item : lista) {
            if (item.get("slug").asText().equals(slug)) {
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
