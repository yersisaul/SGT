package cfbd.co.sgt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base común del CMS web (E2): GET /api/public/web/** es público, el resto
 * de métodos no; el sitio web tiene su propio contador de rate limit; los
 * permisos web_* llegan al Administrador y no a los demás roles.
 */
@SpringBootTest(properties = {
        "app.rate-limit.requests-per-minute=5000",
        "app.rate-limit.auth-requests-per-minute=1000",
        "app.rate-limit.public-requests-per-minute=3"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebPublicoBaseIntegrationTest {

    private static final String RUTA_PUBLICA = "/api/public/web/ruta-de-prueba";
    private static final List<String> PERMISOS_WEB = List.of(
            "web_catalogo.read", "web_catalogo.create", "web_catalogo.update", "web_catalogo.delete",
            "web_comercial.read", "web_comercial.create", "web_comercial.update", "web_comercial.delete",
            "web_novedad.read", "web_novedad.create", "web_novedad.update", "web_novedad.delete",
            "web_novedad.publish",
            "web_documento.read", "web_documento.create", "web_documento.update", "web_documento.delete");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.default-password}")
    private String seedPassword;

    private String tokenAdministrador;
    private int rafagas;

    @BeforeAll
    void login() throws Exception {
        tokenAdministrador = login("administrador1@cfbd.co");
    }

    @Test
    void soloGetEsPublicoYLosDemasMetodosExigenToken() throws Exception {
        // Sin endpoints todavía (llegan en E3): 404 y no 401 prueba que la
        // seguridad dejó pasar la petición anónima. El rate limit va aparte.
        mockMvc.perform(post(RUTA_PUBLICA).with(desdeIp("10.0.0.1"))).andExpect(status().isUnauthorized());
        mockMvc.perform(put(RUTA_PUBLICA).with(desdeIp("10.0.0.1"))).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(RUTA_PUBLICA).with(desdeIp("10.0.0.1"))).andExpect(status().isUnauthorized());
        // Fuera de /api/public/web/** nada cambia.
        mockMvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void elSitioWebTieneSuPropioLimiteSinAfectarAlResto() throws Exception {
        // El límite es por minuto calendario: si el minuto cambia en medio de
        // la ráfaga, el contador se reinicia y se repite una vez (con otra IP).
        List<Integer> estados = rafagaPublica();
        if (!estados.contains(429)) {
            estados = rafagaPublica();
        }
        assertThat(estados.subList(0, 3)).containsOnly(404);
        assertThat(estados.get(3)).isEqualTo(429);

        // El resto de la API (otro contador) sigue respondiendo.
        mockMvc.perform(get("/api/permisos").header("Authorization", "Bearer " + tokenAdministrador))
                .andExpect(status().isOk());
    }

    @Test
    void losPermisosWebLleganSoloAlAdministrador() throws Exception {
        assertThat(permisosDelToken(tokenAdministrador)).containsAll(PERMISOS_WEB);
        for (String email : List.of("cliente1@cfbd.co", "despachador1@cfbd.co", "operaciones1@cfbd.co")) {
            assertThat(permisosDelToken(login(email))).doesNotContainAnyElementsOf(PERMISOS_WEB);
        }
    }

    @Test
    void corsPermitePatchDesdeUnOrigenAutorizado() throws Exception {
        // Preflight del navegador antes de un PATCH (publicar, habilitar…). El
        // origen debe estar en CORS_ALLOWED_ORIGINS (.env incluye el :5500).
        MvcResult preflight = mockMvc.perform(options("/api/web/productos/" + UUID.randomUUID() + "/publicacion")
                        .header("Origin", "http://127.0.0.1:5500")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "authorization,content-type")
                        .with(desdeIp("10.0.2.1")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(preflight.getResponse().getHeader("Access-Control-Allow-Methods")).contains("PATCH");
        assertThat(preflight.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo("http://127.0.0.1:5500");
    }

    // ---------- Helpers ----------

    // IP propia por ráfaga: cada prueba usa un contador limpio del rate limit.
    private List<Integer> rafagaPublica() throws Exception {
        String ip = "10.0.1." + (++rafagas);
        List<Integer> estados = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            estados.add(mockMvc.perform(get(RUTA_PUBLICA).with(desdeIp(ip))).andReturn().getResponse().getStatus());
        }
        return estados;
    }

    private RequestPostProcessor desdeIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private List<String> permisosDelToken(String token) throws Exception {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        List<String> permisos = new ArrayList<>();
        objectMapper.readTree(payload).get("permisos").forEach(p -> permisos.add(p.asText()));
        return permisos;
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", seedPassword))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
