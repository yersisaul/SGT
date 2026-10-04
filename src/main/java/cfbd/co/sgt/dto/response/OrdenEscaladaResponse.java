package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * OT que superó el umbral de espera en cola (GET /ordenes/escaladas).
 *
 * @param nivel 1 = avisada a los responsables; 2 = avisada también al Administrador.
 */
public record OrdenEscaladaResponse(UUID id_orden, String numero_orden, UUID id_especialidad, String especialidad,
                                    String prioridad, Instant en_cola_desde, long minutos_en_cola, int nivel) {
}
