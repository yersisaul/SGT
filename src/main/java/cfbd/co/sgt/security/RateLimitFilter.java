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
 * RATE_LIMIT_REQUESTS_PER_MINUTE / AUTH_RATE_LIMIT_PER_MINUTE /
 * PUBLIC_RATE_LIMIT_PER_MINUTE. Cada grupo (auth, sitio web público y resto
 * de la API) tiene su propio contador, así el tráfico del sitio web no
 * consume el cupo de quien usa el SGT desde la misma IP.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String PREFIJO_AUTH = "/api/auth/";
    private static final String PREFIJO_PUBLICO = "/api/public/";

    private record Ventana(long minuto, AtomicInteger contador) {
    }

    private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();
    private final int limiteGeneralPorMinuto;
    private final int limiteAuthPorMinuto;
    private final int limitePublicoPorMinuto;

    public RateLimitFilter(
            @Value("${app.rate-limit.requests-per-minute:60}") int limiteGeneralPorMinuto,
            @Value("${app.rate-limit.auth-requests-per-minute:10}") int limiteAuthPorMinuto,
            @Value("${app.rate-limit.public-requests-per-minute:120}") int limitePublicoPorMinuto) {
        this.limiteGeneralPorMinuto = limiteGeneralPorMinuto;
        this.limiteAuthPorMinuto = limiteAuthPorMinuto;
        this.limitePublicoPorMinuto = limitePublicoPorMinuto;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String grupo;
        int limite;
        if (uri.startsWith(PREFIJO_AUTH)) {
            grupo = "auth:";
            limite = limiteAuthPorMinuto;
        } else if (uri.startsWith(PREFIJO_PUBLICO)) {
            grupo = "public:";
            limite = limitePublicoPorMinuto;
        } else {
            grupo = "api:";
            limite = limiteGeneralPorMinuto;
        }
        String clave = grupo + request.getRemoteAddr();

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
