package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.AuthResponse;
import cfbd.co.sgt.dto.LoginRequest;
import cfbd.co.sgt.dto.RegistroClienteRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    /** Crea un usuario "Cliente" deshabilitado; si el email ya existe no hace nada. */
    void registrarCliente(RegistroClienteRequest request);
}
