package cfbd.co.sgt.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tareas periódicas (heartbeat SSE, escalado, aviso de SLA) y envío asíncrono de correos. */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}
