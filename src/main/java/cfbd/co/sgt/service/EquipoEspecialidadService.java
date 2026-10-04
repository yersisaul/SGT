package cfbd.co.sgt.service;

import java.util.List;
import java.util.UUID;

import cfbd.co.sgt.dto.request.EquipoEspecialidadRequest;
import cfbd.co.sgt.dto.response.MiEspecialidadResponse;
import cfbd.co.sgt.dto.response.MiembroEspecialidadResponse;

/** Equipos por especialidad: miembros y responsables (PRD E2). */
public interface EquipoEspecialidadService {

    List<MiembroEspecialidadResponse> listarMiembros(UUID idEspecialidad);

    List<MiembroEspecialidadResponse> reemplazarMiembros(UUID idEspecialidad, EquipoEspecialidadRequest request);

    List<MiEspecialidadResponse> listarMisEspecialidades();
}
