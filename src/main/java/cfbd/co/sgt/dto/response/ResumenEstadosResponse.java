package cfbd.co.sgt.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumenEstadosResponse {
    private long total;
    private List<EstadoCantidadResponse> porEstado;
}
