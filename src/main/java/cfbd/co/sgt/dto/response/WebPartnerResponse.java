package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.UUID;

import cfbd.co.sgt.model.TipoPartner;
import lombok.Getter;
import lombok.Setter;

/** Partner para el panel de administración (incluye no publicados y auditoría). */
@Getter
@Setter
public class WebPartnerResponse {
    private UUID id_partner;
    private String nombre;
    private String categoria;
    private String descripcion;
    private String url_logo;
    private String url_sitio;
    private TipoPartner tipo;
    private int orden;
    private boolean publicado;
    private Instant fecha_creacion;
    private Instant fecha_actualizacion;
    private String nombre_usuario_actualiza;
}
