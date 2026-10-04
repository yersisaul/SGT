package cfbd.co.sgt.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo de PUT /especialidades/{id}/miembros: reemplaza el equipo completo
 * (miembros y responsables) de la especialidad. Una lista vacía deja la
 * especialidad sin miembros.
 */
@Getter
@Setter
public class EquipoEspecialidadRequest {
    @NotNull
    @Valid
    private List<MiembroEspecialidadRequest> miembros = new ArrayList<>();
}
