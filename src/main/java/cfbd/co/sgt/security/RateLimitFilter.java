package cfbd.co.sgt.security;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limiting centralizado por ventana fija (1 minuto) en memoria, por
 * IP de cliente. No requiere dependencias externas ni caché distribuido;
 * suficiente para una instancia única. Límite configurable vía
 * RATE_LIMIT_REQUESTS_PER_MINUTE / AUTH_RATE_LIMIT_PER_MINUTE.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String PREFIJO_AUTH = "/api/auth/";

    private record Ventana(long minuto, AtomicInteger contador) {
    }

    private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();
    private final int limiteGeneralPorMinuto;
    private final int limiteAuthPorMinuto;

    public RateLimitFilter(
            @Value("${app.rate-limit.requests-per-minute:60}") int limiteGeneralPorMinuto,
            @Value("${app.rate-limit.auth-requests-per-minute:10}") int limiteAuthPorMinuto) {
        this.limiteGeneralPorMinuto = limiteGeneralPorMinuto;
        this.limiteAuthPorMinuto = limiteAuthPorMinuto;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        boolean esAuth = request.getRequestURI().startsWith(PREFIJO_AUTH);
        int limite = esAuth ? limiteAuthPorMinuto : limiteGeneralPorMinuto;
        String clave = (esAuth ? "auth:" : "api:") + request.getRemoteAddr();

        long minutoActual = System.currentTimeMillis() / 60_000;
        Ventana ventana = ventanas.compute(clave, (k, actual) ->
                (actual == null || actual.minuto() != minutoActual)
                        ? new Ventana(minutoActual, new AtomicInteger(0))
                        : actual);

        if (ventana.contador().incrementAndGet() > limite) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"message\":\"Demasiadas solicitudes, intente más tarde\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
