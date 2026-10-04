package cfbd.co.sgt.service.kpi;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.KpiIndicadorResponse;
import cfbd.co.sgt.dto.response.KpiResponse;

/**
 * Metas de KPI aprobadas el 2026-10-04 (configurables por variables de
 * entorno; se recalibran a los 60 días con datos reales). Agrega a cada
 * indicador su meta y si cumple, está en alerta o no cumple.
 */
@Component
public class MetasKpi {

    private final Map<String, MetaKpi> metas;

    public MetasKpi(@Value("${app.kpi.meta.sla-despacho:90}") double slaDespacho,
                    @Value("${app.kpi.alerta.sla-despacho:80}") double slaDespachoAlerta,
                    @Value("${app.kpi.meta.sla-atencion:85}") double slaAtencion,
                    @Value("${app.kpi.meta.cola-mediana-horas:1}") double colaMediana,
                    @Value("${app.kpi.meta.cola-p90-horas:4}") double colaP90,
                    @Value("${app.kpi.meta.decision-rq:90}") double decisionRq,
                    @Value("${app.kpi.meta.reasignacion-max:10}") double reasignacionMax,
                    @Value("${app.kpi.meta.devolucion-max:15}") double devolucionMax,
                    @Value("${app.kpi.meta.carga-max-persona:5}") double cargaMaxPersona) {
        this.metas = Map.of(
                "sla-despacho.cumplimiento", new MetaKpi(slaDespacho, slaDespachoAlerta, true),
                "sla-atencion.cumplimiento", new MetaKpi(slaAtencion, null, true),
                "tiempos-ciclo.cola_mediana", new MetaKpi(colaMediana, null, false),
                "tiempos-ciclo.cola_p90", new MetaKpi(colaP90, null, false),
                "decision-rq.cumplimiento", new MetaKpi(decisionRq, null, true),
                "ruteo.tasa_reasignacion", new MetaKpi(reasignacionMax, null, false),
                "ruteo.tasa_devolucion", new MetaKpi(devolucionMax, null, false),
                "cola-carga.carga_max", new MetaKpi(cargaMaxPersona, null, false));
    }

    public KpiResponse aplicar(KpiResponse kpi) {
        List<KpiIndicadorResponse> indicadores = kpi.indicadores().stream()
                .map(indicador -> evaluar(kpi.familia(), indicador))
                .toList();
        return new KpiResponse(kpi.familia(), kpi.titulo(), kpi.desde(), kpi.hasta(), kpi.alcance(), kpi.instantaneo(),
                indicadores, kpi.columnas(), kpi.filas());
    }

    private KpiIndicadorResponse evaluar(String familia, KpiIndicadorResponse indicador) {
        MetaKpi meta = metas.get(familia + "." + indicador.clave());
        if (meta == null) {
            return indicador;
        }
        String estado = indicador.valor() == null ? null : meta.evaluar(indicador.valor());
        return new KpiIndicadorResponse(indicador.clave(), indicador.etiqueta(), indicador.valor(), indicador.unidad(),
                meta.valor(), estado);
    }
}
