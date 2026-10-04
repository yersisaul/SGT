package cfbd.co.sgt.service.kpi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.KpiColumnaResponse;
import cfbd.co.sgt.dto.response.KpiIndicadorResponse;
import cfbd.co.sgt.dto.response.KpiResponse;
import cfbd.co.sgt.model.AsignacionOrden;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.TipoAsignacionOrden;
import cfbd.co.sgt.repository.AsignacionOrdenRepository;
import cfbd.co.sgt.repository.OrdenRepository;
import lombok.RequiredArgsConstructor;

/**
 * FR-031: mediana y p90 (horas) por especialidad de las OT registradas en el
 * rango: espera en cola (entra a la cola → la toman o asignan), ejecución
 * (primera toma/asignación → cierre) y punta a punta (registro del origen →
 * cierre). Cada reingreso a la cola (reasignación o devolución) cuenta como
 * una espera nueva.
 */
@Component
@RequiredArgsConstructor
class TiemposCicloKpiCalculator implements KpiCalculator {

    private static final Set<TipoAsignacionOrden> ENTRA_A_COLA = Set.of(TipoAsignacionOrden.ENCOLADA,
            TipoAsignacionOrden.REASIGNADA_ESPECIALIDAD, TipoAsignacionOrden.DEVUELTA);
    private static final Set<TipoAsignacionOrden> SALE_DE_COLA = Set.of(TipoAsignacionOrden.TOMADA,
            TipoAsignacionOrden.ASIGNADA);

    private final OrdenRepository ordenRepository;
    private final AsignacionOrdenRepository asignacionOrdenRepository;

    @Override
    public String familia() {
        return "tiempos-ciclo";
    }

    @Override
    public KpiResponse calcular(AlcanceKpi alcance) {
        List<Orden> ordenes = alcance.esGlobal()
                ? ordenRepository.findRegistradasEntre(alcance.desde(), alcance.hasta())
                : ordenRepository.findRegistradasEntre(alcance.desde(), alcance.hasta(), alcance.idsEspecialidad());
        Map<UUID, List<AsignacionOrden>> eventos = ordenes.isEmpty() ? Map.of()
                : asignacionOrdenRepository.findByOrdenes(ordenes.stream().map(Orden::getId_orden).toList()).stream()
                        .collect(Collectors.groupingBy(a -> a.getOrden().getId_orden()));

        Map<String, TiemposCiclo> porEspecialidad = new LinkedHashMap<>();
        TiemposCiclo total = new TiemposCiclo();
        for (Orden orden : ordenes) {
            TiemposCiclo grupo = porEspecialidad.computeIfAbsent(orden.getEspecialidad().getNombre(), n -> new TiemposCiclo());
            List<AsignacionOrden> historial = eventos.getOrDefault(orden.getId_orden(), List.of());
            for (TiemposCiclo destino : List.of(grupo, total)) {
                acumular(destino, orden, historial);
            }
        }

        List<Map<String, Object>> filas = new ArrayList<>();
        porEspecialidad.forEach((especialidad, tiempos) -> filas.add(tiempos.fila(especialidad)));
        return new KpiResponse(familia(), "Tiempos de ciclo", alcance.desde(), alcance.hasta(), alcance.nombres(), false,
                List.of(
                        KpiIndicadorResponse.de("cola_mediana", "Espera en cola (mediana)", total.mediana(total.cola), "h"),
                        KpiIndicadorResponse.de("cola_p90", "Espera en cola (p90)", total.p90(total.cola), "h"),
                        KpiIndicadorResponse.de("ejecucion_mediana", "Ejecución (mediana)", total.mediana(total.ejecucion), "h"),
                        KpiIndicadorResponse.de("total_mediana", "Punta a punta (mediana)", total.mediana(total.puntaAPunta), "h"),
                        KpiIndicadorResponse.de("ordenes", "OT registradas", (double) total.ordenes, "")),
                List.of(
                        new KpiColumnaResponse("especialidad", "Especialidad", ""),
                        new KpiColumnaResponse("ordenes", "OT", ""),
                        new KpiColumnaResponse("cola_mediana", "Cola (mediana)", "h"),
                        new KpiColumnaResponse("cola_p90", "Cola (p90)", "h"),
                        new KpiColumnaResponse("ejecucion_mediana", "Ejecución (mediana)", "h"),
                        new KpiColumnaResponse("ejecucion_p90", "Ejecución (p90)", "h"),
                        new KpiColumnaResponse("total_mediana", "Punta a punta (mediana)", "h")),
                filas);
    }

    private void acumular(TiemposCiclo tiempos, Orden orden, List<AsignacionOrden> historial) {
        tiempos.ordenes++;
        Instant entradaCola = null;
        Instant primeraToma = null;
        for (AsignacionOrden evento : historial) {
            if (ENTRA_A_COLA.contains(evento.getTipo())) {
                entradaCola = evento.getFecha();
            } else if (SALE_DE_COLA.contains(evento.getTipo()) && entradaCola != null) {
                tiempos.cola.add(Estadistica.horasEntre(entradaCola, evento.getFecha()));
                entradaCola = null;
                primeraToma = primeraToma == null ? evento.getFecha() : primeraToma;
            }
        }
        if (orden.getFecha_cierre() == null) {
            return;
        }
        if (primeraToma != null) {
            tiempos.ejecucion.add(Estadistica.horasEntre(primeraToma, orden.getFecha_cierre()));
        }
        Instant registroOrigen = registroOrigen(orden);
        tiempos.puntaAPunta.add(Estadistica.horasEntre(registroOrigen, orden.getFecha_cierre()));
    }

    private Instant registroOrigen(Orden orden) {
        if (orden.getSolicitud() != null) {
            return orden.getSolicitud().getFecha_registro();
        }
        if (orden.getRequerimiento() != null) {
            return orden.getRequerimiento().getSolicitud() != null
                    ? orden.getRequerimiento().getSolicitud().getFecha_registro()
                    : orden.getRequerimiento().getFecha_registro();
        }
        return orden.getFecha_registro();
    }
}
