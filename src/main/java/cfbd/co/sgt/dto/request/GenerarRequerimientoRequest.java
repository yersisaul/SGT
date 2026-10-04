package cfbd.co.sgt.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo (opcional) de POST /solicitudes/{id}/generar-requerimiento. Si
 * "descripcion" viene vacío, el Service la genera automáticamente a partir
 * de la descripción original de la Solicitud (ver
 * SolicitudServiceImpl.generarRequerimientoDesdeSolicitud) — nunca queda en
 * blanco. Usuario y estado inicial se derivan en el Service.
 * id_especialidad es opcional: si llega, el Despachador cambia la
 * especialidad al despachar (PRD D22); si no, se conserva la de la Solicitud.
 */
@Getter
@Setter
public class GenerarRequerimientoRequest {
    @Size(max = 255)
    private String descripcion;
    private UUID id_especialidad;
}
