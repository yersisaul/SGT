package cfbd.co.sgt.service;

import java.time.LocalDate;
import java.util.UUID;

import cfbd.co.sgt.dto.response.KpiResponse;

/** KPIs del flujo (PRD E6) con rango de fechas y recorte por alcance del actor (FR-035/FR-037). */
public interface KpiService {

    /**
     * @param desde          inicio del rango (inclusive); null = hace 30 días.
     * @param hasta          fin del rango (inclusive); null = hoy.
     * @param idEspecialidad opcional: limita a una especialidad dentro del alcance del actor.
     */
    KpiResponse calcular(String familia, LocalDate desde, LocalDate hasta, UUID idEspecialidad);
}
