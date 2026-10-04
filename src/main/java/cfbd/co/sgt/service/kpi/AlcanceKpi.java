package cfbd.co.sgt.service.kpi;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Rango y alcance de un cálculo de KPI.
 *
 * @param idsEspecialidad null = global (alcance orden.read_all); si no, solo esas especialidades (FR-037).
 */
public record AlcanceKpi(Instant desde, Instant hasta, Collection<UUID> idsEspecialidad, List<String> nombres) {

    public boolean esGlobal() {
        return idsEspecialidad == null;
    }
}
