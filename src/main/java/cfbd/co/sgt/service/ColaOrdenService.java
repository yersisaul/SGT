package cfbd.co.sgt.service;

import java.util.List;
import java.util.UUID;

import cfbd.co.sgt.dto.request.AsignarOrdenRequest;
import cfbd.co.sgt.dto.request.ReasignarOrdenRequest;
import cfbd.co.sgt.dto.request.VerificarOrdenRequest;
import cfbd.co.sgt.dto.response.AsignacionOrdenResponse;
import cfbd.co.sgt.dto.response.CargaMiembroResponse;
import cfbd.co.sgt.dto.response.OrdenResponse;

/**
 * Cola de OT por especialidad (CLAUDE.md §1 pasos 10-12, PRD E3). Es el único
 * componente que cambia el ejecutor o la especialidad de una OT.
 */
public interface ColaOrdenService {

    /** OT "Pendiente" sin ejecutor de las especialidades del actor; para responsables, también las "Devuelta". */
    List<OrdenResponse> listarCola();

    /** OT abiertas por miembro del equipo (responsable de la especialidad o alcance global). */
    List<CargaMiembroResponse> listarCargaEquipo(UUID idEspecialidad);

    OrdenResponse tomar(UUID idOrden);

    OrdenResponse asignar(UUID idOrden, AsignarOrdenRequest request);

    OrdenResponse verificar(UUID idOrden, VerificarOrdenRequest request);

    OrdenResponse reasignar(UUID idOrden, ReasignarOrdenRequest request);

    List<AsignacionOrdenResponse> listarAsignaciones(UUID idOrden);
}
