package cfbd.co.sgt.dto.response;

import lombok.Getter;
import lombok.Setter;

/** Cliente de referencia tal como lo ve el sitio web público: sin estado ni auditoría. */
@Getter
@Setter
public class WebClienteReferenciaPublicoResponse {
    private String nombre;
    private String sector;
    private String url_logo;
    private String url_sitio;
}
