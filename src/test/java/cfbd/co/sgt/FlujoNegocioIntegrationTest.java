package cfbd.co.sgt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Los métodos están ordenados porque comparten estado: son una traza
 * secuencial del flujo real, no unidades independientes.
 */
// El flujo supera el rate limit por defecto en la misma ventana de un
// minuto; se eleva solo para este test.
@SpringBootTest(properties = {
        "app.rate-limit.requests-per-minute=5000",
        "app.rate-limit.auth-requests-per-minute=1000"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlujoNegocioIntegrationTest {

    private static final String EMAIL_OPERACIONES_1 = "operaciones1@cfbd.co";
    private static final String EMAIL_OPERACIONES_2 = "yortiz@cfbd.co";
    private static final String EMAIL_OPERACIONES_3 = "ddiaz@cfbd.co";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.default-password}")
    private String seedPassword;

    private String tokenCliente1;
    private String tokenCliente2;
    private String tokenDespachador;
    private String tokenAdministrador;
    /** Responsable de la especialidad A. */
    private String tokenOperaciones1;
    /** Miembro (no responsable) de la especialidad A. */
    private String tokenOperaciones2;
    /** Responsable de la especialidad B. */
    private String tokenOperaciones3;

    private UUID idOperaciones1;
    private UUID idOperaciones2;
    private UUID idOperaciones3;
    private UUID idEspecialidadA;
    private UUID idEspecialidadB;
    private UUID idActivo;
    private Map<String, UUID> estados;
    private EquiposRespaldo respaldo;

    private UUID idSolicitudBajoContrato;
    private UUID idOrdenBajoContrato;
    private UUID idRequerimientoParaAprobar;
    private UUID idRequerimientoParaRechazar;
    private UUID idRequerimientoPendiente;
    private UUID idOrdenDesdeRequerimiento;
    private UUID idSolicitudFueraDeContrato;
    private UUID idOrdenDesdeRequerimientoDeSolicitud;

    // ---------- FASE 0: identidad, catálogos y equipos ----------

    @Test
    @Order(1)
    void loginCatalogosYEquipos() throws Exception {
        tokenCliente1 = login("cliente1@cfbd.co");
        tokenDespachador = login("despachador1@cfbd.co");
        tokenAdministrador = login("administrador1@cfbd.co");
        tokenOperaciones1 = login(EMAIL_OPERACIONES_1);
        tokenOperaciones2 = login(EMAIL_OPERACIONES_2);
        tokenOperaciones3 = login(EMAIL_OPERACIONES_3);

        JsonNode usuarios = ok(get("/api/usuarios"), tokenAdministrador);
        idOperaciones1 = idUsuarioPorEmail(usuarios, EMAIL_OPERACIONES_1);
        idOperaciones2 = idUsuarioPorEmail(usuarios, EMAIL_OPERACIONES_2);
        idOperaciones3 = idUsuarioPorEmail(usuarios, EMAIL_OPERACIONES_3);

        estados = new HashMap<>();
        for (JsonNode estado : ok(get("/api/estados"), tokenCliente1)) {
            estados.put(estado.get("nombre").asText(), UUID.fromString(estado.get("id_estado").asText()));
        }
        assertThat(estados.keySet()).contains("Pendiente", "En revisión", "En progreso", "Finalizado",
                "Aprobado", "Rechazado", "Asignada", "Devuelta");

        JsonNode especialidades = ok(get("/api/especialidades"), tokenAdministrador);
        assertThat(especialidades.size()).as("Catálogo de 5 especialidades (PRD D4)").isEqualTo(5);
        idEspecialidadA = UUID.fromString(especialidades.get(0).get("id_especialidad").asText());
        idEspecialidadB = UUID.fromString(especialidades.get(1).get("id_especialidad").asText());

        // Equipos: A = operaciones1 (responsable) + yortiz; B = ddiaz (responsable).
        // Se respaldan los equipos reales y se restauran en @AfterAll.
        respaldo = new EquiposRespaldo(mockMvc, objectMapper, tokenAdministrador);
        respaldo.respaldar(idEspecialidadA);
        respaldo.respaldar(idEspecialidadB);
        definirEquipo(idEspecialidadA, Map.of(idOperaciones1, true, idOperaciones2, false));
        definirEquipo(idEspecialidadB, Map.of(idOperaciones3, true));

        // El seeder no siembra activos sin especialidad definida (OQ-10).
        JsonNode activos = ok(get("/api/activos"), tokenCliente1);
        if (activos.isEmpty()) {
            Map<String, Object> activo = new HashMap<>();
            activo.put("id_especialidad", idEspecialidadA.toString());
            activo.put("codigo", "TEST-" + UUID.randomUUID().toString().substring(0, 8));
            activo.put("nombre", "Activo de prueba " + UUID.randomUUID());
            perform(post("/api/activos"), tokenAdministrador, activo, status().isCreated());
            activos = ok(get("/api/activos"), tokenCliente1);
        }
        idActivo = UUID.fromString(activos.get(0).get("id_activo").asText());

        // Segundo Cliente para probar el aislamiento entre clientes.
        UUID idRolCliente = null;
        for (JsonNode rol : ok(get("/api/roles"), tokenAdministrador)) {
            if (rol.get("nombre").asText().equalsIgnoreCase("Cliente")) {
                idRolCliente = UUID.fromString(rol.get("id_rol").asText());
            }
        }
        String sufijo = UUID.randomUUID().toString();
        Map<String, Object> nuevoCliente = new HashMap<>();
        nuevoCliente.put("email", "cliente-ownership-" + sufijo + "@sgt.com");
        nuevoCliente.put("password", seedPassword);
        nuevoCliente.put("nombres", "ClienteOwnership" + sufijo);
        nuevoCliente.put("apellidos", "Dos");
        nuevoCliente.put("id_rol", idRolCliente.toString());
        perform(post("/api/usuarios"), tokenAdministrador, nuevoCliente, status().isCreated());
        tokenCliente2 = login(nuevoCliente.get("email").toString());
    }

    // ---------- FASE 1: pasos 1-2, el Cliente registra y ve solo lo suyo ----------

    @Test
    @Order(2)
    void clienteRegistraYSoloVeSusSolicitudes() throws Exception {
        idSolicitudBajoContrato = crearSolicitud(tokenCliente1, "Alta", "Cámara sin señal, revisar bajo contrato");
        UUID ajena = crearSolicitud(tokenCliente2, "Media", "Solicitud de otro cliente");

        assertThat(contiene(ok(get("/api/solicitudes"), tokenCliente1), "id_solicitud", idSolicitudBajoContrato)).isTrue();
        assertThat(contiene(ok(get("/api/solicitudes"), tokenCliente1), "id_solicitud", ajena)).isFalse();
        perform(get("/api/solicitudes/" + ajena), tokenCliente1, null, status().isNotFound());

        // El Cliente no despacha, no aprueba ni cierra.
        perform(post("/api/solicitudes/" + idSolicitudBajoContrato + "/generar-orden"), tokenCliente1,
                Map.of("id_especialidad", idEspecialidadA.toString()), status().isForbidden());
        perform(post("/api/aprobaciones"), tokenCliente1,
                Map.of("id_requerimiento", UUID.randomUUID().toString(), "aprobado", true), status().isForbidden());
        perform(post("/api/ordenes/" + UUID.randomUUID() + "/cerrar"), tokenCliente1, Map.of(), status().isForbidden());
    }

    // ---------- FASE 2: pasos 3-5, el Despachador genera la OT en la cola ----------

    @Test
    @Order(3)
    void despachadorGeneraOtEnLaColaDeLaEspecialidad() throws Exception {
        // Sin especialidad no se despacha (AC-013).
        perform(post("/api/solicitudes/" + idSolicitudBajoContrato + "/generar-orden"), tokenDespachador,
                Map.of("comentario", "sin especialidad"), status().isBadRequest());

        JsonNode orden = json(perform(post("/api/solicitudes/" + idSolicitudBajoContrato + "/generar-orden"),
                tokenDespachador, Map.of("id_especialidad", idEspecialidadA.toString(),
                        "comentario", "Cubierto por contrato vigente"), status().isCreated()));
        idOrdenBajoContrato = UUID.fromString(orden.get("id_orden").asText());
        assertThat(orden.get("id_usuario").isNull()).as("La OT nace en cola, sin ejecutor").isTrue();
        assertThat(orden.get("id_especialidad").asText()).isEqualTo(idEspecialidadA.toString());
        assertThat(orden.get("id_estado").asText()).isEqualTo(estados.get("Pendiente").toString());
        assertThat(orden.get("numeroOrden").asText()).startsWith("OT-");

        JsonNode solicitud = ok(get("/api/solicitudes/" + idSolicitudBajoContrato), tokenDespachador);
        assertThat(solicitud.get("id_estado").asText()).isEqualTo(estados.get("En progreso").toString());
        assertThat(solicitud.get("id_especialidad").asText()).isEqualTo(idEspecialidadA.toString());

        perform(post("/api/solicitudes/" + idSolicitudBajoContrato + "/generar-orden"), tokenDespachador,
                Map.of("id_especialidad", idEspecialidadA.toString()), status().isConflict());
    }

    // ---------- FASE 3: pasos 10-12, cola, toma, devolución y reasignación ----------

    @Test
    @Order(4)
    void colaTomaDevolucionYReasignacion() throws Exception {
        String ot = "/api/ordenes/" + idOrdenBajoContrato;

        // AC-020: los miembros de A la ven en su cola; los de B no.
        assertThat(contiene(ok(get("/api/ordenes/cola"), tokenOperaciones2), "id_orden", idOrdenBajoContrato)).isTrue();
        assertThat(contiene(ok(get("/api/ordenes/cola"), tokenOperaciones3), "id_orden", idOrdenBajoContrato)).isFalse();
        perform(post(ot + "/tomar"), tokenOperaciones3, null, status().isNotFound());

        // AC-014: un miembro la toma; un segundo intento falla.
        JsonNode tomada = json(perform(post(ot + "/tomar"), tokenOperaciones2, null, status().isOk()));
        assertThat(tomada.get("id_usuario").asText()).isEqualTo(idOperaciones2.toString());
        assertThat(tomada.get("id_estado").asText()).isEqualTo(estados.get("Asignada").toString());
        perform(post(ot + "/tomar"), tokenOperaciones1, null, status().isConflict());

        // El PUT genérico ya no mueve estados.
        perform(put(ot), tokenOperaciones2, Map.of("id_estado", estados.get("En progreso").toString()),
                status().isConflict());

        // AC-016: solo el ejecutor verifica; "no me corresponde" exige motivo y la devuelve.
        perform(post(ot + "/verificar"), tokenOperaciones1, Map.of("corresponde", true), status().isForbidden());
        perform(post(ot + "/verificar"), tokenOperaciones2, Map.of("corresponde", false), status().isBadRequest());
        JsonNode devuelta = json(perform(post(ot + "/verificar"), tokenOperaciones2,
                Map.of("corresponde", false, "motivo", "Es un tema de infraestructura"), status().isOk()));
        assertThat(devuelta.get("id_usuario").isNull()).isTrue();
        assertThat(devuelta.get("id_estado").asText()).isEqualTo(estados.get("Devuelta").toString());
        perform(post(ot + "/tomar"), tokenOperaciones2, null, status().isConflict());

        // La "Devuelta" solo aparece en la cola del responsable.
        assertThat(contiene(ok(get("/api/ordenes/cola"), tokenOperaciones1), "id_orden", idOrdenBajoContrato)).isTrue();
        assertThat(contiene(ok(get("/api/ordenes/cola"), tokenOperaciones2), "id_orden", idOrdenBajoContrato)).isFalse();

        // AC-017: reasigna el responsable (no un miembro), con motivo y a otra especialidad.
        perform(post(ot + "/reasignar"), tokenOperaciones2,
                Map.of("id_especialidad_destino", idEspecialidadB.toString(), "motivo", "x"), status().isForbidden());
        perform(post(ot + "/reasignar"), tokenOperaciones1,
                Map.of("id_especialidad_destino", idEspecialidadB.toString()), status().isBadRequest());
        perform(post(ot + "/reasignar"), tokenOperaciones1,
                Map.of("id_especialidad_destino", idEspecialidadA.toString(), "motivo", "misma"), status().isConflict());
        JsonNode reasignada = json(perform(post(ot + "/reasignar"), tokenOperaciones1,
                Map.of("id_especialidad_destino", idEspecialidadB.toString(), "motivo", "Corresponde a B"),
                status().isOk()));
        assertThat(reasignada.get("id_especialidad").asText()).isEqualTo(idEspecialidadB.toString());
        assertThat(reasignada.get("id_estado").asText()).isEqualTo(estados.get("Pendiente").toString());
        assertThat(contiene(ok(get("/api/ordenes/cola"), tokenOperaciones3), "id_orden", idOrdenBajoContrato)).isTrue();
        assertThat(contiene(ok(get("/api/ordenes/cola"), tokenOperaciones2), "id_orden", idOrdenBajoContrato)).isFalse();

        // AC-015: el responsable de B asigna solo a miembros de B.
        perform(post(ot + "/asignar"), tokenOperaciones3, Map.of("id_usuario", idOperaciones2.toString()),
                status().isConflict());
        JsonNode asignada = json(perform(post(ot + "/asignar"), tokenOperaciones3,
                Map.of("id_usuario", idOperaciones3.toString()), status().isOk()));
        assertThat(asignada.get("id_estado").asText()).isEqualTo(estados.get("Asignada").toString());

        // Carga del equipo: solo el responsable de la especialidad.
        JsonNode carga = ok(get("/api/ordenes/equipo/" + idEspecialidadB), tokenOperaciones3);
        assertThat(carga.get(0).get("ordenes_abiertas").asLong()).isGreaterThanOrEqualTo(1);
        perform(get("/api/ordenes/equipo/" + idEspecialidadA), tokenOperaciones2, null, status().isForbidden());
    }

    // ---------- FASE 4: pasos 13-14, ejecutar y cerrar ----------

    @Test
    @Order(5)
    void ejecutorConfirmaYCierraConCascada() throws Exception {
        String ot = "/api/ordenes/" + idOrdenBajoContrato;
        // Solo se cierra lo confirmado ("En progreso").
        perform(post(ot + "/cerrar"), tokenOperaciones3, Map.of(), status().isConflict());
        perform(post(ot + "/verificar"), tokenOperaciones3, Map.of("corresponde", true), status().isOk());
        // En progreso ya no se reasigna, ni siquiera el responsable.
        perform(post(ot + "/reasignar"), tokenOperaciones3,
                Map.of("id_especialidad_destino", idEspecialidadA.toString(), "motivo", "tarde"), status().isConflict());

        perform(post(ot + "/cerrar"), tokenDespachador, Map.of(), status().isForbidden());
        perform(post(ot + "/cerrar"), tokenOperaciones1, Map.of(), status().isNotFound());
        JsonNode cerrada = json(perform(post(ot + "/cerrar"), tokenOperaciones3,
                Map.of("comentario", "Trabajo completado en sitio"), status().isOk()));
        assertThat(cerrada.get("fecha_cierre").isNull()).isFalse();
        assertThat(cerrada.get("id_estado").asText()).isEqualTo(estados.get("Finalizado").toString());
        perform(post(ot + "/cerrar"), tokenOperaciones3, Map.of(), status().isConflict());

        JsonNode solicitud = ok(get("/api/solicitudes/" + idSolicitudBajoContrato), tokenCliente1);
        assertThat(solicitud.get("id_estado").asText()).isEqualTo(estados.get("Finalizado").toString());

        // AC-018: trazabilidad completa de asignaciones.
        List<String> tipos = new ArrayList<>();
        for (JsonNode asignacion : ok(get(ot + "/asignaciones"), tokenAdministrador)) {
            tipos.add(asignacion.get("tipo").asText());
        }
        assertThat(tipos).containsExactly("ENCOLADA", "TOMADA", "DEVUELTA", "REASIGNADA_ESPECIALIDAD",
                "ASIGNADA", "CONFIRMADA");
    }

    // ---------- FASE 5: pasos 6-9, Requerimientos ----------

    @Test
    @Order(6)
    void administradorApruebaRechazaYGeneraOtManual() throws Exception {
        idRequerimientoParaAprobar = crearRequerimiento(tokenDespachador, "Instalación fuera de contrato");
        idRequerimientoParaRechazar = crearRequerimiento(tokenDespachador, "Fuera de alcance");
        idRequerimientoPendiente = crearRequerimiento(tokenDespachador, "Queda en revisión");

        perform(post("/api/aprobaciones"), tokenDespachador,
                Map.of("id_requerimiento", idRequerimientoParaAprobar.toString(), "aprobado", true), status().isForbidden());

        perform(post("/api/aprobaciones"), tokenAdministrador,
                Map.of("id_requerimiento", idRequerimientoParaRechazar.toString(), "aprobado", false,
                        "comentario", "Fuera de alcance del servicio"), status().isCreated());
        assertThat(ok(get("/api/requerimientos/" + idRequerimientoParaRechazar), tokenAdministrador)
                .get("id_estado").asText()).isEqualTo(estados.get("Rechazado").toString());
        perform(post("/api/requerimientos/" + idRequerimientoParaRechazar + "/generar-orden"), tokenAdministrador,
                Map.of("id_especialidad", idEspecialidadA.toString()), status().isConflict());
        perform(post("/api/requerimientos/" + idRequerimientoPendiente + "/generar-orden"), tokenAdministrador,
                Map.of("id_especialidad", idEspecialidadA.toString()), status().isConflict());

        // AC-024: aprobar sin especialidad (modal cancelado) deja "Aprobado" sin OT.
        perform(post("/api/aprobaciones"), tokenAdministrador,
                Map.of("id_requerimiento", idRequerimientoParaAprobar.toString(), "aprobado", true,
                        "comentario", "Aprobado; OT luego"), status().isCreated());
        assertThat(ok(get("/api/requerimientos/" + idRequerimientoParaAprobar), tokenAdministrador)
                .get("id_estado").asText()).isEqualTo(estados.get("Aprobado").toString());

        perform(post("/api/requerimientos/" + idRequerimientoParaAprobar + "/generar-orden"), tokenDespachador,
                Map.of("id_especialidad", idEspecialidadA.toString()), status().isForbidden());
        JsonNode orden = json(perform(post("/api/requerimientos/" + idRequerimientoParaAprobar + "/generar-orden"),
                tokenAdministrador, Map.of("id_especialidad", idEspecialidadB.toString()), status().isCreated()));
        idOrdenDesdeRequerimiento = UUID.fromString(orden.get("id_orden").asText());
        assertThat(orden.get("id_usuario").isNull()).isTrue();
        assertThat(orden.get("id_especialidad").asText()).isEqualTo(idEspecialidadB.toString());
        assertThat(ok(get("/api/requerimientos/" + idRequerimientoParaAprobar), tokenAdministrador)
                .get("id_estado").asText()).isEqualTo(estados.get("En progreso").toString());
        perform(post("/api/requerimientos/" + idRequerimientoParaAprobar + "/generar-orden"), tokenAdministrador,
                Map.of("id_especialidad", idEspecialidadB.toString()), status().isConflict());

        // Operaciones no aprueba, no ve RQ ajenos a sus OT y no administra usuarios.
        perform(post("/api/aprobaciones"), tokenOperaciones1,
                Map.of("id_requerimiento", idRequerimientoPendiente.toString(), "aprobado", true), status().isForbidden());
        perform(put("/api/requerimientos/" + idRequerimientoPendiente), tokenOperaciones1,
                Map.of("id_estado", estados.get("Pendiente").toString(), "id_especialidad", idEspecialidadA.toString(),
                        "descripcion", "intento no autorizado"), status().isNotFound());
        perform(get("/api/usuarios"), tokenOperaciones1, null, status().isForbidden());
    }

    // ---------- FASE 6: Solicitud fuera de contrato → RQ → aprobación con OT → cierre ----------

    @Test
    @Order(7)
    void fueraDeContratoAprobacionConOtYCierreEnCascada() throws Exception {
        String descripcion = "El cliente solicita mantenimiento fuera del alcance contratado";
        idSolicitudFueraDeContrato = crearSolicitud(tokenCliente1, "Media", descripcion);
        String numero = ok(get("/api/solicitudes/" + idSolicitudFueraDeContrato), tokenDespachador)
                .get("numeroSolicitud").asText();

        JsonNode rq = json(perform(post("/api/solicitudes/" + idSolicitudFueraDeContrato + "/generar-requerimiento"),
                tokenDespachador, Map.of("id_especialidad", idEspecialidadB.toString()), status().isCreated()));
        UUID idRq = UUID.fromString(rq.get("id_requerimiento").asText());
        assertThat(rq.get("descripcion").asText()).contains(descripcion).contains(numero);
        assertThat(rq.get("id_especialidad").asText()).isEqualTo(idEspecialidadB.toString());
        assertThat(rq.get("id_estado").asText()).isEqualTo(estados.get("En revisión").toString());
        assertThat(ok(get("/api/solicitudes/" + idSolicitudFueraDeContrato), tokenDespachador)
                .get("id_estado").asText()).isEqualTo(estados.get("En revisión").toString());

        perform(post("/api/solicitudes/" + idSolicitudFueraDeContrato + "/generar-orden"), tokenDespachador,
                Map.of("id_especialidad", idEspecialidadA.toString()), status().isConflict());

        // AC-023: aprobar con especialidad (modal confirmado) genera la OT en la misma operación.
        perform(post("/api/aprobaciones"), tokenAdministrador,
                Map.of("id_requerimiento", idRq.toString(), "aprobado", true,
                        "comentario", "Aprobado tras revisión de jefatura",
                        "id_especialidad_orden", idEspecialidadA.toString()), status().isCreated());
        assertThat(ok(get("/api/requerimientos/" + idRq), tokenAdministrador).get("id_estado").asText())
                .isEqualTo(estados.get("En progreso").toString());
        assertThat(ok(get("/api/solicitudes/" + idSolicitudFueraDeContrato), tokenDespachador)
                .get("id_estado").asText()).isEqualTo(estados.get("En progreso").toString());

        JsonNode cola = ok(get("/api/ordenes/cola"), tokenOperaciones1);
        for (JsonNode orden : cola) {
            if (orden.get("id_requerimiento") != null && !orden.get("id_requerimiento").isNull()
                    && orden.get("id_requerimiento").asText().equals(idRq.toString())) {
                idOrdenDesdeRequerimientoDeSolicitud = UUID.fromString(orden.get("id_orden").asText());
            }
        }
        assertThat(idOrdenDesdeRequerimientoDeSolicitud).as("La OT del RQ aprobado está en la cola de A").isNotNull();

        String ot = "/api/ordenes/" + idOrdenDesdeRequerimientoDeSolicitud;
        perform(post(ot + "/asignar"), tokenOperaciones1, Map.of("id_usuario", idOperaciones2.toString()), status().isOk());
        perform(post(ot + "/verificar"), tokenOperaciones2, Map.of("corresponde", true), status().isOk());
        perform(post(ot + "/cerrar"), tokenOperaciones2, Map.of("comentario", "Completado"), status().isOk());

        assertThat(ok(get("/api/requerimientos/" + idRq), tokenAdministrador).get("id_estado").asText())
                .isEqualTo(estados.get("Finalizado").toString());
        assertThat(ok(get("/api/solicitudes/" + idSolicitudFueraDeContrato), tokenCliente1).get("id_estado").asText())
                .isEqualTo(estados.get("Finalizado").toString());
    }

    // ---------- FASE 7: paso 8 "NO → FIN", rechazo visible para el Cliente ----------

    @Test
    @Order(8)
    void rechazoDelRequerimientoRechazaLaSolicitudDelCliente() throws Exception {
        UUID idSolicitud = crearSolicitud(tokenCliente1, "Baja", "Pedido que no está en el contrato");
        JsonNode rq = json(perform(post("/api/solicitudes/" + idSolicitud + "/generar-requerimiento"),
                tokenDespachador, null, status().isCreated()));
        perform(post("/api/aprobaciones"), tokenAdministrador,
                Map.of("id_requerimiento", rq.get("id_requerimiento").asText(), "aprobado", false,
                        "comentario", "Fuera de alcance"), status().isCreated());

        // AC-021/AC-022: la Solicitud queda "Rechazado" y el Cliente ve el motivo.
        assertThat(ok(get("/api/solicitudes/" + idSolicitud), tokenCliente1).get("id_estado").asText())
                .isEqualTo(estados.get("Rechazado").toString());
        boolean motivoVisible = false;
        for (JsonNode registro : ok(get("/api/historial-solicitudes?id_solicitud=" + idSolicitud), tokenCliente1)) {
            if (registro.get("comentario").asText().contains("Fuera de alcance")) {
                motivoVisible = true;
            }
        }
        assertThat(motivoVisible).isTrue();
    }

    // ---------- FASE 8: seguridad por recurso e integridad (etapa 1) ----------

    @Test
    @Order(9)
    void seguridadPorRecursoEIntegridad() throws Exception {
        // AC-002: aunque el Cliente envíe "Finalizado", la Solicitud nace "Pendiente".
        Map<String, Object> payload = new HashMap<>();
        payload.put("id_activo", idActivo.toString());
        payload.put("id_estado", estados.get("Finalizado").toString());
        payload.put("prioridad", "Baja");
        payload.put("descripcion", "Intento de saltarse al Despachador");
        JsonNode creada = json(perform(post("/api/solicitudes"), tokenCliente1, payload, status().isCreated()));
        assertThat(creada.get("id_estado").asText()).isEqualTo(estados.get("Pendiente").toString());
        assertThat(creada.get("nombre_usuario").asText()).isNotBlank();

        // AC-003: otro cliente no la edita (404) y el dueño no edita una despachada (409).
        Map<String, Object> edicion = Map.of("id_activo", idActivo.toString(), "prioridad", "Alta",
                "descripcion", "Edición ajena");
        perform(put("/api/solicitudes/" + creada.get("id_solicitud").asText()), tokenCliente2, edicion,
                status().isNotFound());
        perform(put("/api/solicitudes/" + idSolicitudBajoContrato), tokenCliente1, edicion, status().isConflict());

        // AC-004: el Cliente solo ve las OT de sus Solicitudes.
        JsonNode ordenesCliente1 = ok(get("/api/ordenes"), tokenCliente1);
        assertThat(contiene(ordenesCliente1, "id_orden", idOrdenBajoContrato)).isTrue();
        assertThat(contiene(ordenesCliente1, "id_orden", idOrdenDesdeRequerimientoDeSolicitud)).isTrue();
        assertThat(contiene(ordenesCliente1, "id_orden", idOrdenDesdeRequerimiento)).isFalse();
        assertThat(contiene(ok(get("/api/ordenes"), tokenCliente2), "id_orden", idOrdenBajoContrato)).isFalse();
        perform(get("/api/ordenes/" + idOrdenBajoContrato), tokenCliente2, null, status().isNotFound());

        // AC-005: el Cliente no lista usuarios y su historial de OT se limita a las suyas.
        perform(get("/api/usuarios"), tokenCliente1, null, status().isForbidden());
        for (JsonNode registro : ok(get("/api/historial-ordenes"), tokenCliente2)) {
            assertThat(registro.get("id_orden").asText()).isNotEqualTo(idOrdenBajoContrato.toString());
        }
        perform(get("/api/historial-ordenes?id_orden=" + idOrdenBajoContrato), tokenCliente2, null,
                status().isNotFound());

        // AC-025: aprobado nulo -> 400.
        perform(post("/api/aprobaciones"), tokenAdministrador,
                Map.of("id_requerimiento", idRequerimientoPendiente.toString()), status().isBadRequest());

        // AC-039: una Solicitud con historial no se borra.
        perform(delete("/api/solicitudes/" + idSolicitudBajoContrato), tokenAdministrador, null, status().isConflict());
    }

    // ---------- FASE 9: validaciones de equipos (etapa 2) ----------

    @Test
    @Order(10)
    void validacionesDeEquipos() throws Exception {
        JsonNode mias = ok(get("/api/especialidades/mias"), tokenOperaciones1);
        boolean responsableEnA = false;
        for (JsonNode e : mias) {
            if (e.get("id_especialidad").asText().equals(idEspecialidadA.toString())) {
                responsableEnA = e.get("es_responsable").asBoolean();
            }
        }
        assertThat(responsableEnA).isTrue();
        assertThat(ok(get("/api/especialidades/" + idEspecialidadA + "/miembros"), tokenOperaciones1).size()).isEqualTo(2);
        perform(get("/api/especialidades/" + idEspecialidadA + "/miembros"), tokenCliente1, null, status().isForbidden());

        UUID idCliente1 = idUsuarioPorEmail(ok(get("/api/usuarios"), tokenAdministrador), "cliente1@cfbd.co");
        perform(put("/api/especialidades/" + idEspecialidadB + "/miembros"), tokenAdministrador,
                Map.of("miembros", List.of(Map.of("id_usuario", idCliente1.toString(), "es_responsable", false))),
                status().isConflict());
        perform(put("/api/especialidades/" + idEspecialidadB + "/miembros"), tokenAdministrador,
                Map.of("miembros", List.of(
                        Map.of("id_usuario", idOperaciones3.toString(), "es_responsable", false),
                        Map.of("id_usuario", idOperaciones3.toString(), "es_responsable", true))),
                status().isBadRequest());
        perform(put("/api/especialidades/" + idEspecialidadB + "/miembros"), tokenOperaciones1,
                Map.of("miembros", List.of()), status().isForbidden());
    }

    // ---------- FASE 10: concurrencia al tomar (NFR-005) ----------

    @Test
    @Order(11)
    void dosMiembrosTomanLaMismaOtAlMismoTiempo() throws Exception {
        UUID idSolicitud = crearSolicitud(tokenCliente1, "Alta", "Prueba de concurrencia");
        JsonNode orden = json(perform(post("/api/solicitudes/" + idSolicitud + "/generar-orden"), tokenDespachador,
                Map.of("id_especialidad", idEspecialidadA.toString()), status().isCreated()));
        String tomar = "/api/ordenes/" + orden.get("id_orden").asText() + "/tomar";

        java.util.concurrent.CountDownLatch salida = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<Integer>> resultados = new ArrayList<>();
            for (String token : List.of(tokenOperaciones1, tokenOperaciones2)) {
                resultados.add(pool.submit(() -> {
                    salida.await();
                    return mockMvc.perform(post(tomar).header("Authorization", "Bearer " + token))
                            .andReturn().getResponse().getStatus();
                }));
            }
            salida.countDown();
            List<Integer> codigos = new ArrayList<>();
            for (java.util.concurrent.Future<Integer> resultado : resultados) {
                codigos.add(resultado.get(30, java.util.concurrent.TimeUnit.SECONDS));
            }
            assertThat(codigos).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
    }

    @AfterAll
    void restaurarEquipos() throws Exception {
        if (respaldo != null) {
            respaldo.restaurar();
        }
    }

    // ---------- Helpers ----------

    private void definirEquipo(UUID idEspecialidad, Map<UUID, Boolean> miembros) throws Exception {
        List<Map<String, Object>> lista = new ArrayList<>();
        miembros.forEach((idUsuario, responsable) ->
                lista.add(Map.of("id_usuario", idUsuario.toString(), "es_responsable", responsable)));
        perform(put("/api/especialidades/" + idEspecialidad + "/miembros"), tokenAdministrador,
                Map.of("miembros", lista), status().isOk());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", email, "password", seedPassword))))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("accessToken").asText();
    }

    private UUID crearSolicitud(String token, String prioridad, String descripcion) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id_activo", idActivo.toString());
        payload.put("prioridad", prioridad);
        payload.put("descripcion", descripcion);
        return UUID.fromString(json(perform(post("/api/solicitudes"), token, payload, status().isCreated()))
                .get("id_solicitud").asText());
    }

    private UUID crearRequerimiento(String token, String descripcion) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id_estado", estados.get("Pendiente").toString());
        payload.put("id_especialidad", idEspecialidadA.toString());
        payload.put("descripcion", descripcion);
        return UUID.fromString(json(perform(post("/api/requerimientos"), token, payload, status().isCreated()))
                .get("id_requerimiento").asText());
    }

    private MvcResult perform(MockHttpServletRequestBuilder builder, String token, Object body, ResultMatcher esperado)
            throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(toJson(body));
        }
        return mockMvc.perform(builder).andExpect(esperado).andReturn();
    }

    private JsonNode ok(MockHttpServletRequestBuilder builder, String token) throws Exception {
        return json(perform(builder, token, null, status().isOk()));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String toJson(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private boolean contiene(JsonNode lista, String campo, UUID id) {
        for (JsonNode item : lista) {
            if (item.get(campo).asText().equals(id.toString())) {
                return true;
            }
        }
        return false;
    }

    private UUID idUsuarioPorEmail(JsonNode usuarios, String email) {
        for (JsonNode usuario : usuarios) {
            if (usuario.get("email").asText().equalsIgnoreCase(email)) {
                return UUID.fromString(usuario.get("id_usuario").asText());
            }
        }
        return null;
    }
}
