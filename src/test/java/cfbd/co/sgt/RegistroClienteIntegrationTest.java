package cfbd.co.sgt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Auto-registro de clientes desde el sitio web: la cuenta nace deshabilitada
 * con rol Cliente y solo puede iniciar sesión tras habilitarla un administrador.
 */
@SpringBootTest(properties = {
        "app.rate-limit.requests-per-minute=5000",
        "app.rate-limit.auth-requests-per-minute=1000"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistroClienteIntegrationTest {

    private static final String REGISTRO = "/api/auth/registro-cliente";
    private static final String PASSWORD_REGISTRO = "ClaveSegura123";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.default-password}")
    private String seedPassword;

    private String tokenAdministrador;
    private String tokenCliente1;
    /** Usuarios creados por las pruebas, se eliminan en @AfterAll. */
    private final List<UUID> creados = new ArrayList<>();

    @BeforeAll
    void login() throws Exception {
        tokenAdministrador = login("administrador1@cfbd.co", seedPassword, status().isOk());
        tokenCliente1 = login("cliente1@cfbd.co", seedPassword, status().isOk());
    }

    @Test
    void registroQuedaPendienteHastaQueElAdministradorLoHabilita() throws Exception {
        String email = emailNuevo();
        Map<String, Object> body = registro(email);
        // Un rol enviado por el cliente se ignora: siempre es "Cliente".
        body.put("rol", "Administrador");
        body.put("id_rol", UUID.randomUUID().toString());
        registrar(body, status().isAccepted());

        // Contraseña correcta pero cuenta deshabilitada → 403.
        login(email, PASSWORD_REGISTRO, status().isForbidden());
        // Contraseña incorrecta → 401 (no revela que la cuenta existe).
        login(email, "OtraClave999", status().isUnauthorized());

        JsonNode pendiente = buscar(json(perform(get("/api/usuarios?habilitado=false"), tokenAdministrador, null,
                status().isOk())), email);
        assertThat(pendiente).isNotNull();
        assertThat(pendiente.get("habilitado").asBoolean()).isFalse();
        assertThat(pendiente.get("nombre_rol").asText()).isEqualTo("Cliente");
        UUID id = UUID.fromString(pendiente.get("id_usuario").asText());
        creados.add(id);

        JsonNode habilitado = json(perform(patch("/api/usuarios/" + id + "/habilitacion"), tokenAdministrador,
                Map.of("habilitado", true), status().isOk()));
        assertThat(habilitado.get("habilitado").asBoolean()).isTrue();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", email, "password", PASSWORD_REGISTRO))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json(result).get("user").get("role").asText()).isEqualTo("Cliente");
    }

    @Test
    void emailExistenteRespondeIgualYNoAlteraLaCuenta() throws Exception {
        // Mayúsculas: se normaliza y coincide con la cuenta sembrada.
        Map<String, Object> body = registro("Cliente1@CFBD.co");
        registrar(body, status().isAccepted());

        // La cuenta sembrada sigue habilitada y con su contraseña original.
        login("cliente1@cfbd.co", seedPassword, status().isOk());
        login("cliente1@cfbd.co", PASSWORD_REGISTRO, status().isUnauthorized());
    }

    @Test
    void validaLosDatosDelFormulario() throws Exception {
        Map<String, Object> emailInvalido = registro("no-es-un-email");
        registrar(emailInvalido, status().isBadRequest());

        Map<String, Object> passwordCorta = registro(emailNuevo());
        passwordCorta.put("password", "1234567");
        registrar(passwordCorta, status().isBadRequest());

        Map<String, Object> sinNombres = registro(emailNuevo());
        sinNombres.put("nombres", " ");
        registrar(sinNombres, status().isBadRequest());
    }

    @Test
    void habilitacionExigePermisoYNoPermiteDeshabilitarseASiMismo() throws Exception {
        UUID idAdministrador = UUID.fromString(buscar(json(perform(get("/api/usuarios"), tokenAdministrador, null,
                status().isOk())), "administrador1@cfbd.co").get("id_usuario").asText());
        String ruta = "/api/usuarios/" + idAdministrador + "/habilitacion";

        mockMvc.perform(patch(ruta).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("habilitado", false))))
                .andExpect(status().isUnauthorized());
        perform(patch(ruta), tokenCliente1, Map.of("habilitado", false), status().isForbidden());
        perform(patch(ruta), tokenAdministrador, Map.of("habilitado", false), status().isConflict());
        perform(patch(ruta), tokenAdministrador, Map.of(), status().isBadRequest());
    }

    @AfterAll
    void eliminarCreados() throws Exception {
        for (UUID id : creados) {
            perform(delete("/api/usuarios/" + id), tokenAdministrador, null, status().isOk());
        }
    }

    // ---------- Helpers ----------

    private String emailNuevo() {
        return "registro-web-" + UUID.randomUUID() + "@sgt.com";
    }

    private Map<String, Object> registro(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("nombres", "Registro");
        body.put("apellidos", "Web");
        body.put("email", email);
        body.put("password", PASSWORD_REGISTRO);
        return body;
    }

    private void registrar(Map<String, Object> body, ResultMatcher esperado) throws Exception {
        mockMvc.perform(post(REGISTRO).contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(esperado);
    }

    private String login(String email, String password, ResultMatcher esperado) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", email, "password", password))))
                .andExpect(esperado)
                .andReturn();
        JsonNode token = result.getResponse().getStatus() == 200 ? json(result).get("accessToken") : null;
        return token == null ? null : token.asText();
    }

    private JsonNode buscar(JsonNode usuarios, String email) {
        for (JsonNode usuario : usuarios) {
            if (usuario.get("email").asText().equals(email)) {
                return usuario;
            }
        }
        return null;
    }

    private MvcResult perform(MockHttpServletRequestBuilder builder, String token, Object body, ResultMatcher esperado)
            throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(toJson(body));
        }
        return mockMvc.perform(builder).andExpect(esperado).andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String toJson(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }
}
