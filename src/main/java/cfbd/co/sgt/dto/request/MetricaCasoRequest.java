package cfbd.co.sgt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cifra destacada de un caso de éxito ("50 000" / "vecinos registrados"). */
public record MetricaCasoRequest(

        @NotBlank(message = "El valor de la métrica es obligatorio")
        @Size(max = 30, message = "El valor de la métrica no puede superar 30 caracteres")
        String valor,

        @NotBlank(message = "La etiqueta de la métrica es obligatoria")
        @Size(max = 100, message = "La etiqueta de la métrica no puede superar 100 caracteres")
        String etiqueta) {
}
