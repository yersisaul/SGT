package cfbd.co.sgt.dto.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** Categoría de novedades. En el sitio público id_categoria_novedad no se usa: se filtra por slug. */
@Getter
@Setter
public class WebCategoriaNovedadResponse {
    private UUID id_categoria_novedad;
    private String nombre;
    private String slug;
    private int orden;
}
