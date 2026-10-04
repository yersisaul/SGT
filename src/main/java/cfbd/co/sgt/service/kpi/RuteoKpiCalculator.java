package cfbd.co.sgt.service.kpi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.KpiColumnaResponse;
import cfbd.co.sgt.dto.response.KpiIndicadorResponse;
import cfbd.co.sgt.dto.response.KpiResponse;
import cfbd.co.sgt.model.Aprobacion;
import cfbd.co.sgt.model.AsignacionOrden;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.TipoAsignacionOrden;
import cfbd.co.sgt.repository.AprobacionRepository;
import cfbd.co.sgt.repository.AsignacionOrdenRepository;
import cfbd.co.sgt.repository.OrdenRepository;
import lombok.RequiredArgsConstructor;

/**
 * FR-033: calidad del ruteo. Para las OT registradas en el rango, cuántas se
 * devolvieron o se reasignaron de especialidad, agrupadas por la especialidad
 * a la que se encolaron primero (la clasificación del Despachador/Admin).
 * Además, aprobación vs rechazo de los RQ decididos en el rango.
 */
@Component
@RequiredArgsConstructor
class RuteoKpiCalculator implements KpiCalculator {

    private final OrdenRepository ordenRepository;
    private final AsignacionOrdenRepository asignacionOrdenRepository;
    private final AprobacionRepository aprobacionRepository;

    @Override
    public String familia() {
        return "ruteo";
    }

    @Override
    public KpiResponse calcular(AlcanceKpi alcance) {
        List<Orden> ordenes = alcance.esGlobal()
                ? ordenRepository.findRegistradasEntre(alcance.desde(), alcance.hasta())
                : ordenRepository.findRegistradasEntre(alcance.desde(), alcance.hasta(), alcance.idsEspecialidad());
        Map<UUID, List<AsignacionOrden>> eventos = ordenes.isEmpty() ? Map.of()
                : asignacionOrdenRepository.findByOrdenes(ordenes.stream().map(Orden::getId_orden).toList()).stream()
                        .collect(Collectors.groupingBy(a -> a.getOrden().getId_orden()));

        Map<String, long[]> porEspecialidad = new LinkedHashMap<>();
        long reasignadas = 0;
        long devueltas = 0;
        for (Orden orden : ordenes) {
            List<AsignacionOrden> historial = eventos.getOrDefault(orden.getId_orden(), List.of());
            String especialidadInicial = historial.stream()
                    .filter(e -> e.getTipo() == TipoAsignacionOrden.ENCOLADA)
                    .findFirst()
                    .map(e -> e.getEspecialidad_destino().getNombre())
                    .orElse(orden.getEspecialidad().getNombre());
            boolean fueReasignada = tiene(historial, TipoAsignacionOrden.REASIGNADA_ESPECIALIDAD);
            boolean fueDevuelta = tiene(historial, TipoAsignacionOrden.DEVUELTA);
            long[] conteo = porEspecialidad.computeIfAbsent(especialidadInicial, e -> new long[3]);
            conteo[0]++;
            conteo[1] += fueDevuelta ? 1 : 0;
            conteo[2] += fueReasignada ? 1 : 0;
            reasignadas += fueReasignada ? 1 : 0;
            devueltas += fueDevuelta ? 1 : 0;
        }

        List<Aprobacion> decisiones = alcance.esGlobal()
                ? aprobacionRepository.findDecididasEntre(alcance.desde(), alcance.hasta())
                : aprobacionRepository.findDecididasEntre(alcance.desde(), alcance.hasta(), alcance.idsEspecialidad());
        long aprobados = decisiones.stream().filter(a -> Boolean.TRUE.equals(a.getAprobado())).count();

        List<Map<String, Object>> filas = new ArrayList<>();
        porEspecialidad.forEach((especialidad, conteo) -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("especialidad", especialidad);
            fila.put("generadas", conteo[0]);
            fila.put("devueltas", conteo[1]);
            fila.put("reasignadas", conteo[2]);
            fila.put("tasa_reasignacion", Estadistica.porcentaje(conteo[2], conteo[0]));
            filas.add(fila);
        });
        return new KpiResponse(familia(), "Calidad del ruteo", alcance.desde(), alcance.hasta(), alcance.nombres(), false,
                List.of(
                        KpiIndicadorResponse.de("tasa_reasignacion", "Reasignadas de especialidad",
                                Estadistica.porcentaje(reasignadas, ordenes.size()), "%"),
                        KpiIndicadorResponse.de("tasa_devolucion", "Devueltas por el ejecutor",
                                Estadistica.porcentaje(devueltas, ordenes.size()), "%"),
                        KpiIndicadorResponse.de("tasa_aprobacion_rq", "RQ aprobados",
                                Estadistica.porcentaje(aprobados, decisiones.size()), "%"),
                        KpiIndicadorResponse.de("rq_decididos", "RQ decididos", (double) decisiones.size(), "")),
                List.of(
                        new KpiColumnaResponse("especialidad", "Especialidad inicial", ""),
                        new KpiColumnaResponse("generadas", "OT generadas", ""),
                        new KpiColumnaResponse("devueltas", "Devueltas", ""),
                        new KpiColumnaResponse("reasignadas", "Reasignadas", ""),
                        new KpiColumnaResponse("tasa_reasignacion", "Tasa de reasignación", "%")),
                filas);
    }

    private boolean tiene(List<AsignacionOrden> historial, TipoAsignacionOrden tipo) {
        return historial.stream().anyMatch(e -> e.getTipo() == tipo);
    }
}
