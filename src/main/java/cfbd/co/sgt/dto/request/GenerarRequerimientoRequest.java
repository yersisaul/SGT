package cfbd.co.sgt.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo (opcional) de POST /solicitudes/{id}/generar-requerimiento. Si
 * "descripcion" viene vacío, el Service la genera automáticamente a partir
 * de la descripción original de la Solicitud (ver
 * SolicitudServiceImpl.generarRequerimientoDesdeSolicitud) — nunca queda en
 * blanco. Especialidad, usuario y estado inicial se derivan siempre en el
 * Service, igual que en GenerarOrdenRequest.
 */
@Getter
@Setter
public class GenerarRequerimientoRequest {
    private String descripcion;
}
