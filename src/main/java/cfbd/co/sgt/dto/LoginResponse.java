package cfbd.co.sgt.dto;

import java.util.UUID;

public record LoginResponse(UUID id, String email, String nombres, String apellidos, String role) {
}
