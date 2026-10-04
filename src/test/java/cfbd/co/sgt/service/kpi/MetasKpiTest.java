package cfbd.co.sgt.service.kpi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import cfbd.co.sgt.dto.response.KpiIndicadorResponse;
import cfbd.co.sgt.dto.response.KpiResponse;

class MetasKpiTest {

    private final MetasKpi metas = new MetasKpi(90, 80, 85, 1, 4, 90, 10, 15, 5);

    @Test
    void slaDeDespachoCumpleAlertaYNoCumple() {
        assertThat(estado("sla-despacho", "cumplimiento", 92.0)).isEqualTo("cumple");
        assertThat(estado("sla-despacho", "cumplimiento", 85.0)).isEqualTo("alerta");
        assertThat(estado("sla-despacho", "cumplimiento", 70.0)).isEqualTo("no_cumple");
    }

    @Test
    void enTiemposYTasasMenorEsMejor() {
        assertThat(estado("tiempos-ciclo", "cola_mediana", 0.5)).isEqualTo("cumple");
        assertThat(estado("tiempos-ciclo", "cola_p90", 6.0)).isEqualTo("no_cumple");
        assertThat(estado("ruteo", "tasa_reasignacion", 10.0)).isEqualTo("cumple");
        assertThat(estado("cola-carga", "carga_max", 6.0)).isEqualTo("no_cumple");
    }

    @Test
    void sinDatosOSinMetaNoHayEstado() {
        assertThat(estado("sla-atencion", "cumplimiento", null)).isNull();
        KpiIndicadorResponse sinMeta = aplicar("ruteo", "tasa_aprobacion_rq", 40.0);
        assertThat(sinMeta.meta()).isNull();
        assertThat(sinMeta.estado()).isNull();
    }

    private String estado(String familia, String clave, Double valor) {
        return aplicar(familia, clave, valor).estado();
    }

    private KpiIndicadorResponse aplicar(String familia, String clave, Double valor) {
        KpiResponse kpi = new KpiResponse(familia, "t", Instant.EPOCH, Instant.EPOCH, List.of("Global"), false,
                List.of(KpiIndicadorResponse.de(clave, "x", valor, "%")), List.of(), List.of());
        return metas.aplicar(kpi).indicadores().get(0);
    }
}
