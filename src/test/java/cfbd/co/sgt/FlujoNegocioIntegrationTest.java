package cfbd.co.sgt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Verifica de extremo a extremo (contra la BD real vía DataSeeder) el flujo
 * de negocio definitivo: Cliente -> Solicitud -> Despachador -> (OT directa |
 * Requerimiento -> Administrador aprueba/rechaza -> OT) -> Operaciones
 * ejecuta/cierra. Cubre los escenarios de CLAUDE.md sección 19 (ownership,
 * permisos de negocio, transiciones de estado).
 *
 * Los métodos están ordenados porque comparten estado (una Solicitud creada
 * en un paso se usa para generar su Orden en el siguiente, etc.) — no son
 * unidades independientes, sino una traza secuencial del flujo real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlujoNegocioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // La app no registra un bean ObjectMapper propio (SecurityConfig también
    // instancia el suyo manualmente); se crea uno local para (de)serializar
    // en el test.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.default-password}")
    private String seedPassword;

    private String tokenCliente1;
    private String tokenCliente2;
    private String tokenDespachador;
    private String tokenAdministrador;
    private String tokenOperaciones;

    private UUID idActivo;
    private UUID idEstadoPendiente;

    private UUID idSolicitudBajoContrato;
    private UUID idSolicitudDeCliente2;

    private UUID idRequerimientoParaAprobar;
    private UUID idRequerimientoParaRechazar;
    private UUID idRequerimientoPendiente;

    private UUID idOrdenDesdeSolicitud;
    private UUID idOrdenDesdeRequerimiento;

    // ---------- FASE 0: identidad y catálogos ----------

    @Test
    @Order(1)
    void loginYCatalogosBase() throws Exception {
        tokenCliente1 = login("cliente1@sgt.com");
        tokenDespachador = login("despachador1@sgt.com");
        tokenAdministrador = login("administrador1@sgt.com");
        tokenOperaciones = login("operaciones1@sgt.com");

        JsonNode activos = json(mockMvc.perform(authed(get("/api/activos"), tokenCliente1))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(activos.isArray()).isTrue();
        assertThat(activos.size()).isGreaterThan(0);
        idActivo = UUID.fromString(activos.get(0).get("id_activo").asText());

        JsonNode estados = json(mockMvc.perform(authed(get("/api/estados"), tokenCliente1))
                .andExpect(status().isOk())
                .andReturn());
        idEstadoPendiente = idEstadoPorNombre(estados, "Pendiente");
        assertThat(idEstadoPendiente).isNotNull();
        assertThat(idEstadoPorNombre(estados, "Aprobado")).as("Estado 'Aprobado' debe existir").isNotNull();
        assertThat(idEstadoPorNombre(estados, "Rechazado")).as("Estado 'Rechazado' debe existir").isNotNull();

        // Segundo Cliente para probar ownership (Administrador crea usuarios).
        JsonNode roles = json(mockMvc.perform(authed(get("/api/roles"), tokenAdministrador))
                .andExpect(status().isOk())
                .andReturn());
        UUID idRolCliente = idRolPorNombre(roles, "Cliente");
        assertThat(idRolCliente).as("Rol 'Cliente' debe existir").isNotNull();
        // 'Gerente' ya no debe existir como rol asignable en el modelo vigente.
        assertThat(idRolPorNombre(roles, "Gerente")).isNull();
        assertThat(idRolPorNombre(roles, "Soporte")).isNull();
        assertThat(idRolPorNombre(roles, "Desarrollador")).isNull();

        Map<String, Object> nuevoCliente = new HashMap<>();
        // UsuarioServiceImpl exige "nombres" único (no solo el email) — se usa un
        // sufijo también en nombres para no chocar con el "Cliente" ya sembrado.
        String sufijo = UUID.randomUUID().toString();
        nuevoCliente.put("email", "cliente-ownership-" + sufijo + "@sgt.com");
        nuevoCliente.put("password", seedPassword);
        nuevoCliente.put("nombres", "ClienteOwnership" + sufijo);
        nuevoCliente.put("apellidos", "Dos");
        nuevoCliente.put("id_rol", idRolCliente.toString());
        mockMvc.perform(authed(post("/api/usuarios"), tokenAdministrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(nuevoCliente)))
                .andExpect(status().isCreated());
        String emailCliente2 = nuevoCliente.get("email").toString();
        tokenCliente2 = login(emailCliente2);
    }

    // ---------- FASE 1: Cliente crea y solo ve sus propias Solicitudes ----------

    @Test
    @Order(2)
    void clientePuedeCrearYListarSusPropiasSolicitudes() throws Exception {
        idSolicitudBajoContrato = crearSolicitud(tokenCliente1, "Alta", "Cámara sin señal, revisar bajo contrato");

        JsonNode misSolicitudes = json(mockMvc.perform(authed(get("/api/solicitudes"), tokenCliente1))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(misSolicitudes.isArray()).isTrue();
        boolean todasSonDeCliente1 = true;
        for (JsonNode s : misSolicitudes) {
            // No exponemos el id_usuario del solicitante para comparar directamente aquí;
            // basta con que la propia solicitud creada aparezca en el listado.
            if (s.get("id_solicitud").asText().equals(idSolicitudBajoContrato.toString())) {
                todasSonDeCliente1 = true;
            }
        }
        assertThat(todasSonDeCliente1).isTrue();
    }

    @Test
    @Order(3)
    void clienteNoPuedeVerSolicitudDeOtroCliente() throws Exception {
        idSolicitudDeCliente2 = crearSolicitud(tokenCliente2, "Media", "Solicitud de otro cliente, no debe ser visible");

        // cliente1 no debe poder consultarla por id (ownership -> 404, no revela existencia).
        mockMvc.perform(authed(get("/api/solicitudes/" + idSolicitudDeCliente2), tokenCliente1))
                .andExpect(status().isNotFound());

        // Tampoco debe aparecer en su listado.
        JsonNode misSolicitudes = json(mockMvc.perform(authed(get("/api/solicitudes"), tokenCliente1))
                .andExpect(status().isOk())
                .andReturn());
        for (JsonNode s : misSolicitudes) {
            assertThat(s.get("id_solicitud").asText()).isNotEqualTo(idSolicitudDeCliente2.toString());
        }
    }

    @Test
    @Order(4)
    void clienteNoPuedeOperarFueraDeSuAlcance() throws Exception {
        mockMvc.perform(authed(post("/api/solicitudes/" + idSolicitudBajoContrato + "/generar-orden"), tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        Map<String, Object> aprobacion = Map.of("id_requerimiento", UUID.randomUUID().toString(), "aprobado", true);
        mockMvc.perform(authed(post("/api/aprobaciones"), tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(aprobacion)))
                .andExpect(status().isForbidden());

        mockMvc.perform(authed(post("/api/ordenes/" + UUID.randomUUID() + "/cerrar"), tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ---------- FASE 2: Despachador genera OT directa (bajo contrato) ----------

    @Test
    @Order(5)
    void despachadorGeneraOrdenDesdeSolicitudBajoContrato() throws Exception {
        MvcResult resultado = mockMvc.perform(authed(post("/api/solicitudes/" + idSolicitudBajoContrato + "/generar-orden"),
                        tokenDespachador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("comentario", "Cubierto por contrato vigente"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode orden = json(resultado);
        idOrdenDesdeSolicitud = UUID.fromString(orden.get("id_orden").asText());
        assertThat(orden.get("id_solicitud").asText()).isEqualTo(idSolicitudBajoContrato.toString());
        assertThat(orden.get("numeroOrden").asText()).startsWith("OT-");
        assertThat(orden.get("fecha_cierre").isNull()).isTrue();

        // Generar la OT ya no finaliza la Solicitud: queda "En progreso"
        // mientras la Orden avanza, y solo llega a "Finalizado" cuando la
        // Orden se cierra (ver el cierre más abajo, en Fase 5).
        JsonNode solicitud = json(mockMvc.perform(authed(get("/api/solicitudes/" + idSolicitudBajoContrato), tokenDespachador))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode estados = json(mockMvc.perform(authed(get("/api/estados"), tokenDespachador)).andReturn());
        assertThat(solicitud.get("id_estado").asText())
                .isEqualTo(idEstadoPorNombre(estados, "En progreso").toString());

        // No se puede generar una segunda Orden desde la misma Solicitud.
        mockMvc.perform(authed(post("/api/solicitudes/" + idSolicitudBajoContrato + "/generar-orden"), tokenDespachador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    // ---------- FASE 3: Despachador crea Requerimientos (fuera de contrato) ----------

    @Test
    @Order(6)
    void despachadorCreaRequerimientosYNoPuedeAprobar() throws Exception {
        idRequerimientoParaAprobar = crearRequerimiento(tokenDespachador, "Instalación fuera de contrato, requiere cotización");
        idRequerimientoParaRechazar = crearRequerimiento(tokenDespachador, "Solicitud fuera de alcance, probablemente se rechace");
        idRequerimientoPendiente = crearRequerimiento(tokenDespachador, "Requerimiento que se queda pendiente de revisión");

        Map<String, Object> aprobacion = Map.of("id_requerimiento", idRequerimientoParaAprobar.toString(), "aprobado", true);
        mockMvc.perform(authed(post("/api/aprobaciones"), tokenDespachador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(aprobacion)))
                .andExpect(status().isForbidden());

        mockMvc.perform(authed(post("/api/requerimientos/" + idRequerimientoParaAprobar + "/generar-orden"), tokenDespachador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ---------- FASE 4: Administrador aprueba/rechaza y genera OT ----------

    @Test
    @Order(7)
    void administradorApruebaRechazaYGeneraOrden() throws Exception {
        // Rechazo real: transiciona el Requerimiento y bloquea la generación de OT.
        Map<String, Object> rechazo = new HashMap<>();
        rechazo.put("id_requerimiento", idRequerimientoParaRechazar.toString());
        rechazo.put("aprobado", false);
        rechazo.put("comentario", "Fuera de alcance del servicio");
        mockMvc.perform(authed(post("/api/aprobaciones"), tokenAdministrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(rechazo)))
                .andExpect(status().isCreated());

        JsonNode estados = json(mockMvc.perform(authed(get("/api/estados"), tokenAdministrador)).andReturn());
        JsonNode requerimientoRechazado = json(mockMvc.perform(
                        authed(get("/api/requerimientos/" + idRequerimientoParaRechazar), tokenAdministrador))
                .andReturn());
        assertThat(requerimientoRechazado.get("id_estado").asText())
                .isEqualTo(idEstadoPorNombre(estados, "Rechazado").toString());

        mockMvc.perform(authed(post("/api/requerimientos/" + idRequerimientoParaRechazar + "/generar-orden"),
                        tokenAdministrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());

        // Un Requerimiento pendiente (nunca revisado) tampoco puede generar OT.
        mockMvc.perform(authed(post("/api/requerimientos/" + idRequerimientoPendiente + "/generar-orden"),
                        tokenAdministrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());

        // Aprobación real.
        Map<String, Object> aprobacion = new HashMap<>();
        aprobacion.put("id_requerimiento", idRequerimientoParaAprobar.toString());
        aprobacion.put("aprobado", true);
        aprobacion.put("comentario", "Aprobado, cotización aceptada por el cliente");
        mockMvc.perform(authed(post("/api/aprobaciones"), tokenAdministrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(aprobacion)))
                .andExpect(status().isCreated());

        JsonNode requerimientoAprobado = json(mockMvc.perform(
                        authed(get("/api/requerimientos/" + idRequerimientoParaAprobar), tokenAdministrador))
                .andReturn());
        assertThat(requerimientoAprobado.get("id_estado").asText())
                .isEqualTo(idEstadoPorNombre(estados, "Aprobado").toString());

        // Despachador no puede generar la OT del Requerimiento aprobado (no es su función).
        mockMvc.perform(authed(post("/api/requerimientos/" + idRequerimientoParaAprobar + "/generar-orden"),
                        tokenDespachador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        MvcResult resultadoOrden = mockMvc.perform(authed(
                        post("/api/requerimientos/" + idRequerimientoParaAprobar + "/generar-orden"), tokenAdministrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode orden = json(resultadoOrden);
        idOrdenDesdeRequerimiento = UUID.fromString(orden.get("id_orden").asText());
        assertThat(orden.get("id_requerimiento").asText()).isEqualTo(idRequerimientoParaAprobar.toString());

        // No se puede generar una segunda Orden desde el mismo Requerimiento.
        mockMvc.perform(authed(post("/api/requerimientos/" + idRequerimientoParaAprobar + "/generar-orden"),
                        tokenAdministrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    // ---------- FASE 5: Operaciones ejecuta y cierra la OT ----------

    @Test
    @Order(8)
    void operacionesEjecutaYCierraLaOrdenSinPrivilegiosIndebidos() throws Exception {
        // No puede aprobar/rechazar.
        Map<String, Object> aprobacion = Map.of("id_requerimiento", idRequerimientoPendiente.toString(), "aprobado", true);
        mockMvc.perform(authed(post("/api/aprobaciones"), tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(aprobacion)))
                .andExpect(status().isForbidden());

        // No puede editar libremente un Requerimiento.
        Map<String, Object> edicion = Map.of(
                "id_estado", idEstadoPendiente.toString(),
                "id_especialidad", idActivo.toString(),
                "descripcion", "intento no autorizado");
        mockMvc.perform(authed(put("/api/requerimientos/" + idRequerimientoPendiente), tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(edicion)))
                .andExpect(status().isForbidden());

        // No administra usuarios.
        mockMvc.perform(authed(get("/api/usuarios"), tokenOperaciones))
                .andExpect(status().isForbidden());

        // Ejecuta: actualiza el estado de la Orden generada desde la Solicitud.
        JsonNode estados = json(mockMvc.perform(authed(get("/api/estados"), tokenOperaciones)).andReturn());
        UUID idEnProgreso = idEstadoPorNombre(estados, "En progreso");
        Map<String, Object> ejecucion = new HashMap<>();
        ejecucion.put("id_estado", idEnProgreso.toString());
        mockMvc.perform(authed(put("/api/ordenes/" + idOrdenDesdeSolicitud), tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(ejecucion)))
                .andExpect(status().isOk());

        // No puede saltar a "Finalizado" mediante el PUT genérico.
        UUID idFinalizado = idEstadoPorNombre(estados, "Finalizado");
        Map<String, Object> saltoIndebido = new HashMap<>();
        saltoIndebido.put("id_estado", idFinalizado.toString());
        mockMvc.perform(authed(put("/api/ordenes/" + idOrdenDesdeSolicitud), tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(saltoIndebido)))
                .andExpect(status().isConflict());

        // Despachador no puede cerrar la OT (no es su función).
        mockMvc.perform(authed(post("/api/ordenes/" + idOrdenDesdeSolicitud + "/cerrar"), tokenDespachador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Cierre real por Operaciones.
        MvcResult cierre = mockMvc.perform(authed(post("/api/ordenes/" + idOrdenDesdeSolicitud + "/cerrar"),
                        tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("comentario", "Trabajo completado en sitio"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode ordenCerrada = json(cierre);
        assertThat(ordenCerrada.get("fecha_cierre").isNull()).isFalse();
        assertThat(ordenCerrada.get("id_estado").asText()).isEqualTo(idFinalizado.toString());

        // Cerrar la Orden finaliza la Solicitud de origen (trazabilidad
        // Solicitud -> OT -> cierre).
        JsonNode solicitudFinalizada = json(mockMvc.perform(
                        authed(get("/api/solicitudes/" + idSolicitudBajoContrato), tokenDespachador))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(solicitudFinalizada.get("id_estado").asText()).isEqualTo(idFinalizado.toString());

        // No se puede cerrar dos veces.
        mockMvc.perform(authed(post("/api/ordenes/" + idOrdenDesdeSolicitud + "/cerrar"), tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());

        // También cierra la Orden que vino del Requerimiento aprobado.
        mockMvc.perform(authed(post("/api/ordenes/" + idOrdenDesdeRequerimiento + "/cerrar"), tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    // ---------- Helpers ----------

    private String login(String email) throws Exception {
        Map<String, Object> body = Map.of("email", email, "password", seedPassword);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("accessToken").asText();
    }

    private UUID crearSolicitud(String token, String prioridad, String descripcion) throws Exception {
        JsonNode estados = json(mockMvc.perform(authed(get("/api/estados"), token)).andReturn());
        UUID idPendiente = idEstadoPorNombre(estados, "Pendiente");
        JsonNode activos = json(mockMvc.perform(authed(get("/api/activos"), token)).andReturn());
        UUID idAct = UUID.fromString(activos.get(0).get("id_activo").asText());
        UUID idEsp = UUID.fromString(activos.get(0).get("id_especialidad").asText());

        Map<String, Object> payload = new HashMap<>();
        payload.put("id_activo", idAct.toString());
        payload.put("id_estado", idPendiente.toString());
        payload.put("id_especialidad", idEsp.toString());
        payload.put("prioridad", prioridad);
        payload.put("descripcion", descripcion);

        MvcResult result = mockMvc.perform(authed(post("/api/solicitudes"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(result).get("id_solicitud").asText());
    }

    private UUID crearRequerimiento(String token, String descripcion) throws Exception {
        JsonNode estados = json(mockMvc.perform(authed(get("/api/estados"), token)).andReturn());
        UUID idPendiente = idEstadoPorNombre(estados, "Pendiente");
        JsonNode activos = json(mockMvc.perform(authed(get("/api/activos"), token)).andReturn());
        UUID idEsp = UUID.fromString(activos.get(0).get("id_especialidad").asText());

        Map<String, Object> payload = new HashMap<>();
        payload.put("id_estado", idPendiente.toString());
        payload.put("id_especialidad", idEsp.toString());
        payload.put("descripcion", descripcion);

        MvcResult result = mockMvc.perform(authed(post("/api/requerimientos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(result).get("id_requerimiento").asText());
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String toJson(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private UUID idEstadoPorNombre(JsonNode estados, String nombre) {
        for (JsonNode estado : estados) {
            if (estado.get("nombre").asText().equalsIgnoreCase(nombre)) {
                return UUID.fromString(estado.get("id_estado").asText());
            }
        }
        return null;
    }

    private UUID idRolPorNombre(JsonNode roles, String nombre) {
        for (JsonNode rol : roles) {
            if (rol.get("nombre").asText().equalsIgnoreCase(nombre)) {
                return UUID.fromString(rol.get("id_rol").asText());
            }
        }
        return null;
    }
}
