package cfbd.co.sgt.dto.response;

import cfbd.co.sgt.model.TipoPartner;
import lombok.Getter;
import lombok.Setter;

/** Partner tal como lo ve el sitio web público: sin estado ni auditoría. */
@Getter
@Setter
public class WebPartnerPublicoResponse {
    private String nombre;
    private String categoria;
    private String descripcion;
    private String url_logo;
    private String url_sitio;
    private TipoPartner tipo;
}
