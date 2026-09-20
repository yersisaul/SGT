package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.AuthResponse;
import cfbd.co.sgt.dto.LoginRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
