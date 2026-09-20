package cfbd.co.sgt.dto;

public record AuthResponse(String accessToken, String refreshToken, LoginResponse user) {
}
