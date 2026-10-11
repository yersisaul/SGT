package cfbd.co.sgt.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cfbd.co.sgt.dto.AuthResponse;
import cfbd.co.sgt.dto.LoginRequest;
import cfbd.co.sgt.dto.MessageResponse;
import cfbd.co.sgt.dto.RegistroClienteRequest;
import cfbd.co.sgt.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Público (formulario del sitio web). Siempre 202 con el mismo mensaje,
    // exista o no el email; la cuenta queda deshabilitada hasta que un
    // administrador la habilite (PATCH /api/usuarios/{id}/habilitacion).
    @PostMapping("/registro-cliente")
    public ResponseEntity<MessageResponse> registrarCliente(@Valid @RequestBody RegistroClienteRequest request) {
        authService.registrarCliente(request);
        return ResponseEntity.accepted().body(new MessageResponse(
                "Registro recibido. Un administrador habilitará tu cuenta después de verificar tu compra."));
    }
}
