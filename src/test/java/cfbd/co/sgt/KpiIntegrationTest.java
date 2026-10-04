package cfbd.co.sgt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** AC-034 a AC-037: KPIs con rango, alcance por rol y exportación CSV. */
@SpringBootTest(properties = {
        "app.rate-limit.requests-per-minute=5000",
        "app.rate-limit.auth-requests-per-minute=1000"
})
@AutoConfigureMockMvc
class KpiIntegrationTest {

    private static final List<String> FAMILIAS = List.of("sla-despacho", "sla-atencion", "tiempos-ciclo", "cola-carga",
            "ruteo", "decision-rq");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.default-password}")
    private String seedPassword;

    @Test
    void administradorVeLasCuatroFamiliasConAlcanceGlobal() throws Exception {
        String admin = login("administrador1@cfbd.co");
        for (String familia : FAMILIAS) {
            JsonNode kpi = json(mockMvc.perform(get("/api/kpis/" + familia).header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(kpi.get("familia").asText()).isEqualTo(familia);
            assertThat(kpi.get("alcance").get(0).asText()).isEqualTo("Global");
            assertThat(kpi.get("indicadores").size()).isGreaterThan(0);
            assertThat(kpi.get("columnas").size()).isGreaterThan(0);
        }
        // Metas aprobadas: el cumplimiento del SLA de despacho trae su meta (90 %).
        JsonNode sla = json(mockMvc.perform(get("/api/kpis/sla-despacho").header("Authorization", "Bearer " + admin))
                .andReturn());
        assertThat(sla.get("indicadores").get(0).get("meta").asDouble()).isEqualTo(90.0);
    }

    @Test
    void rangoInvalidoOFamiliaInexistente() throws Exception {
        // AC-035: desde > hasta o más de 366 días → 400.
        String admin = login("administrador1@cfbd.co");
        LocalDate hoy = LocalDate.now();
        mockMvc.perform(get("/api/kpis/ruteo?desde=" + hoy + "&hasta=" + hoy.minusDays(1))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/kpis/ruteo?desde=" + hoy.minusDays(400) + "&hasta=" + hoy)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/kpis/inexistente").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void alcancePorRol() throws Exception {
        // AC-037: el Cliente no ve KPIs; un responsable solo los de sus especialidades.
        String admin = login("administrador1@cfbd.co");
        String cliente = login("cliente1@cfbd.co");
        String responsable = login("lulloa@cfbd.co");
        String miembro = login("rjuarez@cfbd.co");

        mockMvc.perform(get("/api/kpis/cola-carga").header("Authorization", "Bearer " + cliente))
                .andExpect(status().isForbidden());

        JsonNode usuarios = json(mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + admin)).andReturn());
        JsonNode especialidades = json(mockMvc.perform(get("/api/especialidades").header("Authorization", "Bearer " + admin))
                .andReturn());
        UUID propia = UUID.fromString(especialidades.get(3).get("id_especialidad").asText());
        UUID ajena = UUID.fromString(especialidades.get(4).get("id_especialidad").asText());
        EquiposRespaldo respaldo = new EquiposRespaldo(mockMvc, objectMapper, admin);
        respaldo.respaldar(propia);
        respaldo.respaldar(ajena);
        try {
            verificarAlcanceDeResponsable(admin, responsable, miembro, usuarios, especialidades, propia, ajena);
        } finally {
            respaldo.restaurar();
        }
    }

    private void verificarAlcanceDeResponsable(String admin, String responsable, String miembro, JsonNode usuarios,
                                               JsonNode especialidades, UUID propia, UUID ajena) throws Exception {
        // La especialidad ajena queda sin lulloa como responsable durante la prueba.
        mockMvc.perform(put("/api/especialidades/" + ajena + "/miembros")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("miembros", List.of()))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/especialidades/" + propia + "/miembros")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("miembros", List.of(
                                Map.of("id_usuario", idPorEmail(usuarios, "lulloa@cfbd.co").toString(), "es_responsable", true),
                                Map.of("id_usuario", idPorEmail(usuarios, "rjuarez@cfbd.co").toString(), "es_responsable", false))))))
                .andExpect(status().isOk());

        JsonNode propios = json(mockMvc.perform(get("/api/kpis/cola-carga").header("Authorization", "Bearer " + responsable))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(propios.get("alcance").get(0).asText()).isEqualTo(especialidades.get(3).get("nombre").asText());
        mockMvc.perform(get("/api/kpis/cola-carga?id_especialidad=" + ajena).header("Authorization", "Bearer " + responsable))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/kpis/cola-carga").header("Authorization", "Bearer " + miembro))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportacionCsvSoloConPermiso() throws Exception {
        String admin = login("administrador1@cfbd.co");
        String responsable = login("lulloa@cfbd.co");
        MvcResult csv = mockMvc.perform(get("/api/kpis/sla-despacho/export.csv").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(csv.getResponse().getContentType()).startsWith("text/csv");
        assertThat(csv.getResponse().getHeader("Content-Disposition")).contains("kpi-sla-despacho.csv");
        assertThat(csv.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("Prioridad");
        // Operaciones tiene kpi.read pero no kpi.export.
        mockMvc.perform(get("/api/kpis/sla-despacho/export.csv").header("Authorization", "Bearer " + responsable))
                .andExpect(status().isForbidden());
    }

    private String login(String email) throws Exception {
        return json(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", seedPassword))))
                .andExpect(status().isOk())
                .andReturn()).get("accessToken").asText();
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
