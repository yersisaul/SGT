package cfbd.co.sgt.service;

import java.util.Locale;

/**
 * Forma canónica de un email (sin espacios, en minúsculas). Se aplica al
 * guardar y al iniciar sesión para que "Ana@cfbd.co " y "ana@cfbd.co" sean
 * el mismo usuario.
 */
public final class EmailNormalizador {

    private EmailNormalizador() {
    }

    public static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
