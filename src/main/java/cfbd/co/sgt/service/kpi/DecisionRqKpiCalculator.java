package cfbd.co.sgt.service.kpi;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.KpiColumnaResponse;
import cfbd.co.sgt.dto.response.KpiIndicadorResponse;
import cfbd.co.sgt.dto.response.KpiResponse;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.repository.AprobacionRepository;
import cfbd.co.sgt.repository.RequerimientoRepository;

/**
 * Tiempo de decisión del Administrador sobre los RQ (decisión 2026-10-04):
 * % de RQ registrados en el rango que se aprobaron o rechazaron dentro del
 * plazo (48 h por defecto), por especialidad. Sin decidir y vencido = fuera.
 */
@Component
class DecisionRqKpiCalculator implements KpiCalculator {

    private static final double MEDIANA = 0.5;

    private final RequerimientoRepository requerimientoRepository;
    private final AprobacionRepository aprobacionRepository;
    private final Duration plazo;

    DecisionRqKpiCalculator(RequerimientoRepository requerimientoRepository, AprobacionRepository aprobacionRepository,
                            @Value("${app.kpi.decision-rq-horas:48}") long plazoHoras) {
        this.requerimientoRepository = requerimientoRepository;
        this.aprobacionRepository = aprobacionRepository;
        this.plazo = Duration.ofHours(plazoHoras);
    }

    @Override
    public String familia() {
        return "decision-rq";
    }

    @Override
    public KpiResponse calcular(AlcanceKpi alcance) {
        List<Requerimiento> requerimientos = alcance.esGlobal()
                ? requerimientoRepository.findRegistradosEntre(alcance.desde(), alcance.hasta())
                : requerimientoRepository.findRegistradosEntre(alcance.desde(), alcance.hasta(), alcance.idsEspecialidad());
        Map<UUID, Instant> decisiones = new HashMap<>();
        if (!requerimientos.isEmpty()) {
            for (Object[] fila : aprobacionRepository.primeraDecision(
                    requerimientos.stream().map(Requerimiento::getId_requerimiento).toList())) {
                decisiones.put((UUID) fila[0], (Instant) fila[1]);
            }
        }
        Instant ahora = Instant.now();
        ConteoSla total = new ConteoSla();
        Map<String, ConteoSla> porEspecialidad = new LinkedHashMap<>();
        List<Double> horasDecision = new ArrayList<>();
        for (Requerimiento requerimiento : requerimientos) {
            Instant decision = decisiones.get(requerimiento.getId_requerimiento());
            Instant limite = requerimiento.getFecha_registro().plus(plazo);
            total.registrar(decision, limite, ahora);
            porEspecialidad.computeIfAbsent(requerimiento.getEspecialidad().getNombre(), e -> new ConteoSla())
                    .registrar(decision, limite, ahora);
            if (decision != null) {
                horasDecision.add(Estadistica.horasEntre(requerimiento.getFecha_registro(), decision));
            }
        }
        List<Map<String, Object>> filas = new ArrayList<>();
        porEspecialidad.forEach((especialidad, conteo) -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("especialidad", especialidad);
            fila.put("registrados", conteo.registradas);
            fila.put("decididos", conteo.despachadas);
            fila.put("en_plazo", conteo.dentro);
            fila.put("fuera_plazo", conteo.fuera);
            fila.put("cumplimiento", conteo.cumplimiento());
            filas.add(fila);
        });
        return new KpiResponse(familia(), "Decisión de requerimientos", alcance.desde(), alcance.hasta(),
                alcance.nombres(), false,
                List.of(
                        KpiIndicadorResponse.de("cumplimiento", "Decididos en plazo (" + plazo.toHours() + " h)",
                                total.cumplimiento(), "%"),
                        KpiIndicadorResponse.de("mediana_horas", "Tiempo de decisión (mediana)",
                                Estadistica.percentil(horasDecision, MEDIANA), "h"),
                        KpiIndicadorResponse.de("registrados", "RQ registrados", (double) total.registradas, ""),
                        KpiIndicadorResponse.de("sin_decidir", "Sin decidir", (double) total.sinDespachar, "")),
                List.of(
                        new KpiColumnaResponse("especialidad", "Especialidad", ""),
                        new KpiColumnaResponse("registrados", "Registrados", ""),
                        new KpiColumnaResponse("decididos", "Decididos", ""),
                        new KpiColumnaResponse("en_plazo", "En plazo", ""),
                        new KpiColumnaResponse("fuera_plazo", "Fuera de plazo", ""),
                        new KpiColumnaResponse("cumplimiento", "Cumplimiento", "%")),
                filas);
    }
}
