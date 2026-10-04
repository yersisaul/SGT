package cfbd.co.sgt.service.escalado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

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
 * Escalado de OT en cola (decisión 2026-10-04). Con umbrales en 0 minutos
 * toda OT en cola queda en nivel 2 de inmediato. Usa los equipos sembrados:
 * yortiz es responsable de "Desarrollo"; rjuarez solo es miembro de
 * "Soporte de infraestructura".
 */
@SpringBootTest(properties = {
        "app.rate-limit.requests-per-minute=5000",
        "app.rate-limit.auth-requests-per-minute=1000",
        "app.escalado.alta-minutos=0",
        "app.escalado.media-minutos=0",
        "app.escalado.baja-minutos=0",
        "app.escalado.revision-segundos=3600"
})
@AutoConfigureMockMvc
class EscaladoColaIntegrationTest {

    private static final String ESPECIALIDAD = "Desarrollo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EscaladoColaJob job;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.default-password}")
    private String seedPassword;

    @Test
    void otEnColaSeEscalaYSeAvisaAlResponsableYAlAdministrador() throws Exception {
        String admin = login("administrador1@cfbd.co");
        String despachador = login("despachador1@cfbd.co");
        String cliente = login("cliente1@cfbd.co");
        String responsable = login("yortiz@cfbd.co");
        String soloMiembro = login("rjuarez@cfbd.co");

        String idEspecialidad = null;
        for (JsonNode especialidad : ok(get("/api/especialidades"), admin)) {
            if (especialidad.get("nombre").asText().equals(ESPECIALIDAD)) {
                idEspecialidad = especialidad.get("id_especialidad").asText();
            }
        }
        JsonNode activos = ok(get("/api/activos"), cliente);
        Map<String, Object> solicitud = new HashMap<>();
        solicitud.put("id_activo", activos.get(0).get("id_activo").asText());
        solicitud.put("prioridad", "Alta");
        solicitud.put("descripcion", "Prueba de escalado");
        String idSolicitud = json(send(post("/api/solicitudes"), cliente, solicitud)).get("id_solicitud").asText();
        String numeroOrden = json(send(post("/api/solicitudes/" + idSolicitud + "/generar-orden"), despachador,
                Map.of("id_especialidad", idEspecialidad))).get("numeroOrden").asText();

        assertThat(contieneNivel(ok(get("/api/ordenes/escaladas"), admin), numeroOrden, 2)).isTrue();
        assertThat(contieneNivel(ok(get("/api/ordenes/escaladas"), responsable), numeroOrden, 2)).isTrue();
        assertThat(contieneNivel(ok(get("/api/ordenes/escaladas"), soloMiembro), numeroOrden, 2)).isFalse();
        mockMvc.perform(get("/api/ordenes/escaladas").header("Authorization", "Bearer " + cliente))
                .andExpect(status().isForbidden());

        // El job avisa al Administrador (nivel 2) por el stream.
        MvcResult streamAdmin = mockMvc.perform(get("/api/notificaciones/stream")
                        .header("Authorization", "Bearer " + admin)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();
        job.revisar();
        assertThat(streamAdmin.getResponse().getContentAsString())
                .contains("event:orden.escalada")
                .contains(numeroOrden);
    }

    private boolean contieneNivel(JsonNode escaladas, String numeroOrden, int nivel) {
        for (JsonNode escalada : escaladas) {
            if (escalada.get("numero_orden").asText().equals(numeroOrden) && escalada.get("nivel").asInt() == nivel) {
                return true;
            }
        }
        return false;
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
}
