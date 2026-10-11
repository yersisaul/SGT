package cfbd.co.sgt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Módulo del Panel Azor o analítica de Azor Analytics dentro de un producto. */
public record ModuloProductoRequest(

        @NotBlank(message = "El nombre del módulo es obligatorio")
        @Size(max = 150, message = "El nombre del módulo no puede superar 150 caracteres")
        String nombre,

        @Size(max = 100, message = "El grupo del módulo no puede superar 100 caracteres")
        String grupo,

        @Size(max = 1000, message = "La descripción del módulo no puede superar 1000 caracteres")
        String descripcion) {
}
