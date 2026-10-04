package cfbd.co.sgt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Las pruebas de integración corren contra la BD de desarrollo (decisión
 * D23) y redefinen equipos de especialidad. Este respaldo guarda los equipos
 * reales antes de modificarlos y los restaura al terminar, para no pisar la
 * configuración del dueño del producto.
 */
final class EquiposRespaldo {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final String tokenAdministrador;
    private final Map<UUID, List<Map<String, Object>>> equipos = new LinkedHashMap<>();

    EquiposRespaldo(MockMvc mockMvc, ObjectMapper objectMapper, String tokenAdministrador) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.tokenAdministrador = tokenAdministrador;
    }

    void respaldar(UUID idEspecialidad) throws Exception {
        if (equipos.containsKey(idEspecialidad)) {
            return;
        }
        JsonNode miembros = objectMapper.readTree(mockMvc.perform(get("/api/especialidades/" + idEspecialidad + "/miembros")
                        .header("Authorization", "Bearer " + tokenAdministrador))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        List<Map<String, Object>> lista = new ArrayList<>();
        for (JsonNode miembro : miembros) {
            lista.add(Map.of("id_usuario", miembro.get("id_usuario").asText(),
                    "es_responsable", miembro.get("es_responsable").asBoolean()));
        }
        equipos.put(idEspecialidad, lista);
    }

    void restaurar() throws Exception {
        for (Map.Entry<UUID, List<Map<String, Object>>> equipo : equipos.entrySet()) {
            mockMvc.perform(put("/api/especialidades/" + equipo.getKey() + "/miembros")
                            .header("Authorization", "Bearer " + tokenAdministrador)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("miembros", equipo.getValue()))))
                    .andExpect(status().isOk());
        }
        equipos.clear();
    }
}
