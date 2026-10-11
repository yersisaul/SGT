package cfbd.co.sgt.dto.request;

import cfbd.co.sgt.model.TipoPartner;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Alta/edición de un partner del sitio web. El logo se sube aparte
 * (POST /api/web/partners/{id}/logo). En la edición, tipo, orden y publicado
 * en null conservan su valor actual; en el alta, tipo por defecto TECNOLOGICO.
 */
@Getter
@Setter
public class WebPartnerRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @Size(max = 100, message = "La categoría no puede superar 100 caracteres")
    private String categoria;

    @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
    private String descripcion;

    @Size(max = 500, message = "La URL del sitio no puede superar 500 caracteres")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "La URL del sitio debe empezar con http:// o https://")
    private String url_sitio;

    private TipoPartner tipo;

    @Min(value = 0, message = "El orden no puede ser negativo")
    @Max(value = 9999, message = "El orden no puede superar 9999")
    private Integer orden;

    private Boolean publicado;
}
