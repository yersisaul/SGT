package cfbd.co.sgt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AC-026: al encolarse una OT, los miembros de la especialidad conectados al
 * stream reciben "orden.encolada" en menos de 5 s; los demás no.
 */
@SpringBootTest(properties = {
        "app.rate-limit.requests-per-minute=5000",
        "app.rate-limit.auth-requests-per-minute=1000"
})
@AutoConfigureMockMvc
class NotificacionSseIntegrationTest {

    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(5);
    private static final long PAUSA_SONDEO_MS = 100;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.default-password}")
    private String seedPassword;

    @Test
    void miembroRecibeOrdenEncoladaYNoMiembroNo() throws Exception {
        String admin = login("administrador1@cfbd.co");
        String despachador = login("despachador1@cfbd.co");
        String cliente = login("cliente1@cfbd.co");
        String miembro = login("yortiz@cfbd.co");
        String ajeno = login("rjuarez@cfbd.co");

        JsonNode usuarios = ok(get("/api/usuarios"), admin);
        UUID idMiembro = idPorEmail(usuarios, "yortiz@cfbd.co");
        UUID idEspecialidad = UUID.fromString(ok(get("/api/especialidades"), admin).get(2).get("id_especialidad").asText());
        EquiposRespaldo respaldo = new EquiposRespaldo(mockMvc, objectMapper, admin);
        respaldo.respaldar(idEspecialidad);
        try {
            send(put("/api/especialidades/" + idEspecialidad + "/miembros"), admin,
                    Map.of("miembros", List.of(Map.of("id_usuario", idMiembro.toString(), "es_responsable", false))));
            verificarAviso(admin, despachador, cliente, miembro, ajeno, idEspecialidad);
        } finally {
            respaldo.restaurar();
        }
    }

    private void verificarAviso(String admin, String despachador, String cliente, String miembro, String ajeno,
                                UUID idEspecialidad) throws Exception {

        JsonNode activos = ok(get("/api/activos"), cliente);
        if (activos.isEmpty()) {
            Map<String, Object> activo = new HashMap<>();
            activo.put("id_especialidad", idEspecialidad.toString());
            activo.put("codigo", "SSE-" + UUID.randomUUID().toString().substring(0, 8));
            activo.put("nombre", "Activo SSE " + UUID.randomUUID());
            send(post("/api/activos"), admin, activo);
            activos = ok(get("/api/activos"), cliente);
        }

        MvcResult streamMiembro = abrirStream(miembro);
        MvcResult streamAjeno = abrirStream(ajeno);

        Map<String, Object> solicitud = new HashMap<>();
        solicitud.put("id_activo", activos.get(0).get("id_activo").asText());
        solicitud.put("prioridad", "Alta");
        solicitud.put("descripcion", "Prueba de aviso SSE");
        String idSolicitud = json(send(post("/api/solicitudes"), cliente, solicitud)).get("id_solicitud").asText();
        String numeroOrden = json(send(post("/api/solicitudes/" + idSolicitud + "/generar-orden"), despachador,
                Map.of("id_especialidad", idEspecialidad.toString()))).get("numeroOrden").asText();

        Instant limite = Instant.now().plus(ESPERA_MAXIMA);
        while (!streamMiembro.getResponse().getContentAsString().contains(numeroOrden) && Instant.now().isBefore(limite)) {
            Thread.sleep(PAUSA_SONDEO_MS);
        }
        String recibido = streamMiembro.getResponse().getContentAsString();
        assertThat(recibido).contains("event:orden.encolada").contains(numeroOrden);
        assertThat(recibido).doesNotContain("email").doesNotContain("password");
        assertThat(streamAjeno.getResponse().getContentAsString()).doesNotContain(numeroOrden);
    }

    @Test
    void elStreamExigeAutenticacion() throws Exception {
        mockMvc.perform(get("/api/notificaciones/stream")).andExpect(status().isUnauthorized());
    }

    private MvcResult abrirStream(String token) throws Exception {
        return mockMvc.perform(get("/api/notificaciones/stream")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();
    }

    private String login(String email) throws Exception {
        return json(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", seedPassword))))
                .andExpect(status().isOk())
                .andReturn()).get("accessToken").asText();
    }

    private JsonNode ok(MockHttpServletRequestBuilder builder, String token) throws Exception {
        return json(mockMvc.perform(builder.header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
    }

    private MvcResult send(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        return mockMvc.perform(builder.header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UUID idPorEmail(JsonNode usuarios, String email) {
        for (JsonNode usuario : usuarios) {
            if (usuario.get("email").asText().equalsIgnoreCase(email)) {
                return UUID.fromString(usuario.get("id_usuario").asText());
            }
        }
        return null;
    }
}
