package cfbd.co.sgt.service.kpi;

import cfbd.co.sgt.dto.response.KpiResponse;

/**
 * Una familia de KPI. Agregar una familia nueva = una implementación nueva;
 * KpiServiceImpl las descubre por inyección (Open/Closed).
 */
public interface KpiCalculator {

    /** Identificador usado en la URL: /api/kpis/{familia}. */
    String familia();

    KpiResponse calcular(AlcanceKpi alcance);
}
