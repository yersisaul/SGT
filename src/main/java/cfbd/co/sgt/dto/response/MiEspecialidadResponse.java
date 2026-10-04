package cfbd.co.sgt.dto.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** Especialidad a la que pertenece el usuario autenticado (GET /especialidades/mias). */
@Getter
@Setter
public class MiEspecialidadResponse {
    private UUID id_especialidad;
    private String nombre;
    private Boolean es_responsable;
}
