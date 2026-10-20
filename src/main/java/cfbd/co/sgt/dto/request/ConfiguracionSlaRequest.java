package cfbd.co.sgt.dto.request;

import java.util.List;

import cfbd.co.sgt.model.ClaveSla;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Cuerpo de PUT /api/configuracion/sla: solo se cambian las claves enviadas. */
@Getter
@Setter
public class ConfiguracionSlaRequest {

    @NotEmpty
    @Valid
    private List<Valor> valores;

    @Getter
    @Setter
    public static class Valor {
        @NotNull
        private ClaveSla clave;
        /** Entre 1 hora y 30 días. */
        @NotNull
        @Min(1)
        @Max(720)
        private Integer horas;
    }
}
