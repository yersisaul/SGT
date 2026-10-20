package cfbd.co.sgt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cfbd.co.sgt.repository.RequerimientoRepository;
import cfbd.co.sgt.service.CorreoService;
import cfbd.co.sgt.service.impl.AvisoSlaRequerimientoJob;

/**
 * Observaciones 2026-10-19: el Despachador registra Solicitudes a nombre de
 * un Cliente, SLA editables, descarga CSV y correos informativos (OT
 * recibida y RQ con SLA vencido). El envío real se reemplaza por un mock.
 */
@SpringBootTest(properties = {
        "app.rate-limit.requests-per-minute=5000",
        "app.rate-limit.auth-requests-per-minute=1000"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ObservacionesTicketsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequerimientoRepository requerimientoRepository;

    @Autowired
    private AvisoSlaRequerimientoJob avisoSlaJob;

    @MockitoBean
    private CorreoService correoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.default-password}")
    private String seedPassword;

    private String tokenCliente;
    private String tokenDespachador;
    private String tokenAdministrador;
    private UUID idCliente;
    private UUID idActivo;
    private UUID idEspecialidad;
    private final List<Map<String, Object>> slaOriginal = new ArrayList<>();

    @BeforeAll
    void preparar() throws Exception {
        tokenCliente = login("cliente1@cfbd.co");
        tokenDespachador = login("despachador1@cfbd.co");
        tokenAdministrador = login("administrador1@cfbd.co");
        for (JsonNode usuario : ok(get("/api/usuarios"), tokenAdministrador)) {
            if ("cliente1@cfbd.co".equals(usuario.get("email").asText())) {
                idCliente = UUID.fromString(usuario.get("id_usuario").asText());
            }
        }
        idEspecialidad = UUID.fromString(ok(get("/api/especialidades"), tokenAdministrador).get(0)
                .get("id_especialidad").asText());
        JsonNode activos = ok(get("/api/activos"), tokenCliente);
        if (activos.isEmpty()) {
            Map<String, Object> activo = new HashMap<>();
            activo.put("id_especialidad", idEspecialidad.toString());
            activo.put("codigo", "TEST-" + UUID.randomUUID().toString().substring(0, 8));
            activo.put("nombre", "Activo de prueba " + UUID.randomUUID());
            perform(post("/api/activos"), tokenAdministrador, activo, status().isCreated());
            activos = ok(get("/api/activos"), tokenCliente);
        }
        idActivo = UUID.fromString(activos.get(0).get("id_activo").asText());
        for (JsonNode sla : ok(get("/api/configuracion/sla"), tokenAdministrador)) {
            slaOriginal.add(Map.of("clave", sla.get("clave").asText(), "horas", sla.get("horas").asInt()));
        }
    }

    @AfterAll
    void restaurarSla() throws Exception {
        if (!slaOriginal.isEmpty()) {
            perform(put("/api/configuracion/sla"), tokenAdministrador, Map.of("valores", slaOriginal), status().isOk());
        }
    }

    @Test
    void despachadorRegistraSolicitudANombreDeUnCliente() throws Exception {
        JsonNode solicitud = crearSolicitud(tokenDespachador, idCliente);
        assertThat(solicitud.get("id_usuario").asText()).isEqualTo(idCliente.toString());

        // Sin solicitud.read_all, el id_usuario del body se ignora.
        UUID otro = UUID.fromString(ok(get("/api/usuarios"), tokenAdministrador).get(0).get("id_usuario").asText());
        JsonNode propia = crearSolicitud(tokenCliente, otro);
        assertThat(propia.get("id_usuario").asText()).isEqualTo(idCliente.toString());
    }

    @Test
    void slaEditableSoloConPermisoYSeAplicaAlCalculo() throws Exception {
        perform(get("/api/configuracion/sla"), tokenDespachador, null, status().isForbidden());
        JsonNode slas = ok(get("/api/configuracion/sla"), tokenAdministrador);
        assertThat(slas.size()).isEqualTo(7);

        perform(put("/api/configuracion/sla"), tokenAdministrador,
                Map.of("valores", List.of(Map.of("clave", "DESPACHO_REQUERIMIENTO", "horas", 0))), status().isBadRequest());
        perform(put("/api/configuracion/sla"), tokenDespachador,
                Map.of("valores", List.of(Map.of("clave", "DESPACHO_REQUERIMIENTO", "horas", 5))), status().isForbidden());

        JsonNode actualizados = json(perform(put("/api/configuracion/sla"), tokenAdministrador,
                Map.of("valores", List.of(Map.of("clave", "DESPACHO_REQUERIMIENTO", "horas", 7))), status().isOk()));
        JsonNode rqSla = buscar(actualizados, "DESPACHO_REQUERIMIENTO");
        assertThat(rqSla.get("horas").asInt()).isEqualTo(7);
        assertThat(rqSla.get("modificado_por").isNull()).isFalse();

        JsonNode requerimiento = crearRequerimiento();
        Instant registro = Instant.parse(requerimiento.get("fecha_registro").asText());
        Instant limite = Instant.parse(requerimiento.get("fecha_limite_despacho").asText());
        assertThat(Duration.between(registro, limite)).isEqualTo(Duration.ofHours(7));
    }

    @Test
    void descargaCsvRespetaLaVisibilidad() throws Exception {
        // Registrada a nombre del propio Despachador: el Cliente no debe verla.
        JsonNode solicitud = crearSolicitud(tokenDespachador, null);
        String numero = solicitud.get("numeroSolicitud").asText();

        MvcResult csv = perform(get("/api/solicitudes/exportar"), tokenDespachador, null, status().isOk());
        assertThat(csv.getResponse().getContentType()).startsWith("text/csv");
        assertThat(csv.getResponse().getHeader("Content-Disposition")).contains("solicitudes-");
        String contenido = csv.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(contenido).startsWith("﻿\"Número\",\"Fecha de registro\",\"Solicitante\"");
        assertThat(contenido).contains(numero);

        String ajeno = perform(get("/api/solicitudes/exportar"), tokenCliente, null, status().isOk())
                .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(ajeno).doesNotContain(numero);

        perform(get("/api/requerimientos/exportar"), tokenDespachador, null, status().isOk());
        perform(get("/api/ordenes/exportar"), tokenDespachador, null, status().isOk());
    }

    @Test
    void otEnColaEnviaCorreoALosMiembrosDeLaEspecialidad() throws Exception {
        clearInvocations(correoService);
        JsonNode solicitud = crearSolicitud(tokenDespachador, idCliente);
        JsonNode orden = json(perform(post("/api/solicitudes/" + solicitud.get("id_solicitud").asText() + "/generar-orden"),
                tokenDespachador, Map.of("id_especialidad", idEspecialidad.toString()), status().isCreated()));

        ArgumentCaptor<String> asunto = ArgumentCaptor.forClass(String.class);
        verify(correoService, timeout(5000)).enviar(anyCollection(), asunto.capture(), eq("orden-recibida"), anyMap());
        assertThat(asunto.getValue()).contains(orden.get("numeroOrden").asText());
    }

    @Test
    @SuppressWarnings("unchecked")
    void rqConSlaVencidoAvisaUnaSolaVezALosAdministradores() throws Exception {
        JsonNode requerimiento = crearRequerimiento();
        UUID id = UUID.fromString(requerimiento.get("id_requerimiento").asText());
        String numero = requerimiento.get("numeroRequerimiento").asText();
        // Se simula que lleva más tiempo esperando decisión que cualquier SLA permitido (máx. 720 h).
        requerimientoRepository.findById(id).ifPresent(rq -> {
            rq.setFecha_registro(Instant.now().minus(721, ChronoUnit.HOURS));
            requerimientoRepository.save(rq);
        });

        clearInvocations(correoService);
        avisoSlaJob.revisar();
        ArgumentCaptor<Collection<String>> destinatarios = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(correoService).enviar(destinatarios.capture(), anyString(), eq("requerimiento-sla-vencido"),
                variables.capture());
        assertThat(destinatarios.getValue()).contains("administrador1@cfbd.co");
        assertThat(numeros(variables.getValue())).contains(numero);
        assertThat(requerimientoRepository.findById(id).orElseThrow().getFecha_aviso_sla()).isNotNull();

        // Segunda revisión: ya avisado, no se repite.
        clearInvocations(correoService);
        avisoSlaJob.revisar();
        verify(correoService, org.mockito.Mockito.never()).enviar(anyCollection(), anyString(),
                eq("requerimiento-sla-vencido"), org.mockito.ArgumentMatchers.argThat(
                        (Map<String, Object> mapa) -> numeros(mapa).contains(numero)));
    }

    // ---------- Helpers ----------

    @SuppressWarnings("unchecked")
    private List<Object> numeros(Map<String, Object> variables) {
        return ((List<Map<String, Object>>) variables.get("requerimientos")).stream()
                .map(fila -> fila.get("numero")).toList();
    }

    private JsonNode crearSolicitud(String token, UUID idUsuario) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id_activo", idActivo.toString());
        if (idUsuario != null) {
            payload.put("id_usuario", idUsuario.toString());
        }
        payload.put("prioridad", "Media");
        payload.put("descripcion", "Observaciones 2026-10-19 " + UUID.randomUUID());
        return json(perform(post("/api/solicitudes"), token, payload, status().isCreated()));
    }

    private JsonNode crearRequerimiento() throws Exception {
        return json(perform(post("/api/requerimientos"), tokenDespachador,
                Map.of("id_especialidad", idEspecialidad.toString(), "descripcion", "RQ de prueba " + UUID.randomUUID()),
                status().isCreated()));
    }

    private JsonNode buscar(JsonNode lista, String clave) {
        for (JsonNode item : lista) {
            if (clave.equals(item.get("clave").asText())) {
                return item;
            }
        }
        throw new AssertionError("No se encontró " + clave);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", seedPassword))))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("accessToken").asText();
    }

    private MvcResult perform(MockHttpServletRequestBuilder builder, String token, Object body, ResultMatcher esperado)
            throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder).andExpect(esperado).andReturn();
    }

    private JsonNode ok(MockHttpServletRequestBuilder builder, String token) throws Exception {
        return json(perform(builder, token, null, status().isOk()));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
}
