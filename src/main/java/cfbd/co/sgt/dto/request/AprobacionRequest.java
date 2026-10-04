package cfbd.co.sgt.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AprobacionRequest {
    @NotNull
    private UUID id_requerimiento;
    /** Obligatorio: un nulo antes se interpretaba como rechazo y se persistía nulo. */
    @NotNull
    private Boolean aprobado;
    @Size(max = 255)
    private String comentario;
    private String url_adjunto;
    /**
     * Solo al aprobar: especialidad elegida en el modal (PRD D7/D17). Si llega,
     * la OT se genera en la misma transacción; si no (modal cancelado), el
     * Requerimiento queda "Aprobado" y la OT se genera luego manualmente.
     */
    private UUID id_especialidad_orden;
}
