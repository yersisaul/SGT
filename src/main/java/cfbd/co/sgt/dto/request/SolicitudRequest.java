package cfbd.co.sgt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudRequest {
    /** Ignorado: el solicitante es siempre el usuario autenticado. */
    private UUID id_usuario;
    @NotNull
    private UUID id_activo;
    /** Ignorado al crear (siempre "Pendiente"); opcional al editar. */
    private UUID id_estado;
    /** Ignorada al crear (se toma del activo); al editar solo la usa quien tiene solicitud.read_all. */
    private UUID id_especialidad;
    @NotBlank
    @Size(max = 255)
    private String prioridad;
    @NotBlank
    @Size(max = 255)
    private String descripcion;
    /** Ignorado: el adjunto se sube por /api/archivos/solicitudes/{id}. */
    private String url_adjunto;
}
