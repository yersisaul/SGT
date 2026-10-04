package cfbd.co.sgt.service.escalado;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Umbrales de espera en cola por prioridad (decisión 2026-10-04): nivel 1
 * avisa a los responsables de la especialidad; al doble del tiempo, nivel 2
 * avisa al Administrador. Configurables por variables de entorno.
 */
@Component
public class UmbralesEscalado {

    public static final int SIN_ESCALAR = 0;
    public static final int NIVEL_RESPONSABLE = 1;
    public static final int NIVEL_ADMINISTRADOR = 2;
    private static final int FACTOR_NIVEL_ADMINISTRADOR = 2;

    private final Duration alta;
    private final Duration media;
    private final Duration baja;

    public UmbralesEscalado(@Value("${app.escalado.alta-minutos:30}") long altaMinutos,
                            @Value("${app.escalado.media-minutos:120}") long mediaMinutos,
                            @Value("${app.escalado.baja-minutos:240}") long bajaMinutos) {
        this.alta = Duration.ofMinutes(altaMinutos);
        this.media = Duration.ofMinutes(mediaMinutos);
        this.baja = Duration.ofMinutes(bajaMinutos);
    }

    public Duration umbral(String prioridad) {
        if (prioridad == null) {
            return media;
        }
        return switch (prioridad.trim().toLowerCase()) {
            case "alta" -> alta;
            case "baja" -> baja;
            default -> media;
        };
    }

    /** Nivel de escalado según el tiempo en cola y la prioridad. */
    public int nivel(Duration enCola, String prioridad) {
        Duration umbral = umbral(prioridad);
        if (enCola.compareTo(umbral.multipliedBy(FACTOR_NIVEL_ADMINISTRADOR)) >= 0) {
            return NIVEL_ADMINISTRADOR;
        }
        return enCola.compareTo(umbral) >= 0 ? NIVEL_RESPONSABLE : SIN_ESCALAR;
    }
}
