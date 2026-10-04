package cfbd.co.sgt.service.kpi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.KpiColumnaResponse;
import cfbd.co.sgt.dto.response.KpiIndicadorResponse;
import cfbd.co.sgt.dto.response.KpiResponse;
import cfbd.co.sgt.model.AsignacionOrden;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.TipoAsignacionOrden;
import cfbd.co.sgt.repository.AsignacionOrdenRepository;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.service.EstadoResolver;
import cfbd.co.sgt.service.EstadosNegocio;
import lombok.RequiredArgsConstructor;

/**
 * FR-032: foto actual (no depende del rango) de las OT abiertas por
 * especialidad: en cola, devueltas, en ejecución, antigüedad de la más vieja
 * en cola y la mayor carga individual.
 */
@Component
@RequiredArgsConstructor
class ColaCargaKpiCalculator implements KpiCalculator {

    private static final Set<TipoAsignacionOrden> ENTRA_A_COLA = Set.of(TipoAsignacionOrden.ENCOLADA,
            TipoAsignacionOrden.REASIGNADA_ESPECIALIDAD, TipoAsignacionOrden.DEVUELTA);

    private final OrdenRepository ordenRepository;
    private final AsignacionOrdenRepository asignacionOrdenRepository;

    @Override
    public String familia() {
        return "cola-carga";
    }

    @Override
    public KpiResponse calcular(AlcanceKpi alcance) {
        List<Orden> abiertas = alcance.esGlobal()
                ? ordenRepository.findAbiertas()
                : ordenRepository.findAbiertas(alcance.idsEspecialidad());
        Map<UUID, Instant> entradaCola = ultimaEntradaACola(abiertas);
        Instant ahora = Instant.now();

        Map<String, Map<String, Object>> filas = new LinkedHashMap<>();
        Map<String, Map<UUID, Long>> cargaPorPersona = new HashMap<>();
        // Una persona puede estar en varias especialidades: su carga es la suma.
        Map<UUID, Long> cargaGlobal = new HashMap<>();
        long enCola = 0;
        long devueltas = 0;
        double antiguedadMaxima = 0;
        for (Orden orden : abiertas) {
            String especialidad = orden.getEspecialidad().getNombre();
            Map<String, Object> fila = filas.computeIfAbsent(especialidad, this::filaVacia);
            if (orden.getUsuario() == null) {
                boolean devuelta = EstadoResolver.es(orden.getEstado(), EstadosNegocio.DEVUELTA);
                fila.merge(devuelta ? "devueltas" : "en_cola", 1L, (a, b) -> (Long) a + (Long) b);
                enCola += devuelta ? 0 : 1;
                devueltas += devuelta ? 1 : 0;
                double horas = Estadistica.redondear(Estadistica.horasEntre(
                        entradaCola.getOrDefault(orden.getId_orden(), orden.getFecha_registro()), ahora));
                fila.merge("antiguedad_max", horas, (a, b) -> Math.max((Double) a, (Double) b));
                antiguedadMaxima = Math.max(antiguedadMaxima, horas);
            } else {
                fila.merge("con_ejecutor", 1L, (a, b) -> (Long) a + (Long) b);
                cargaPorPersona.computeIfAbsent(especialidad, e -> new HashMap<>())
                        .merge(orden.getUsuario().getId_usuario(), 1L, Long::sum);
                cargaGlobal.merge(orden.getUsuario().getId_usuario(), 1L, Long::sum);
            }
        }
        cargaPorPersona.forEach((especialidad, carga) ->
                filas.get(especialidad).put("carga_max", carga.values().stream().mapToLong(Long::longValue).max().orElse(0)));

        return new KpiResponse(familia(), "Cola y carga", alcance.desde(), alcance.hasta(), alcance.nombres(), true,
                List.of(
                        KpiIndicadorResponse.de("en_cola", "OT en cola", (double) enCola, ""),
                        KpiIndicadorResponse.de("devueltas", "OT devueltas", (double) devueltas, ""),
                        KpiIndicadorResponse.de("antiguedad_max", "Más antigua en cola", abiertas.isEmpty() ? null : antiguedadMaxima, "h"),
                        KpiIndicadorResponse.de("abiertas", "OT abiertas", (double) abiertas.size(), ""),
                        KpiIndicadorResponse.de("carga_max", "Mayor carga por persona",
                                (double) cargaGlobal.values().stream().mapToLong(Long::longValue).max().orElse(0), "")),
                List.of(
                        new KpiColumnaResponse("especialidad", "Especialidad", ""),
                        new KpiColumnaResponse("en_cola", "En cola", ""),
                        new KpiColumnaResponse("devueltas", "Devueltas", ""),
                        new KpiColumnaResponse("con_ejecutor", "Con ejecutor", ""),
                        new KpiColumnaResponse("antiguedad_max", "Más antigua en cola", "h"),
                        new KpiColumnaResponse("carga_max", "Mayor carga individual", "")),
                new ArrayList<>(filas.values()));
    }

    private Map<String, Object> filaVacia(String especialidad) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("especialidad", especialidad);
        fila.put("en_cola", 0L);
        fila.put("devueltas", 0L);
        fila.put("con_ejecutor", 0L);
        fila.put("antiguedad_max", null);
        fila.put("carga_max", 0L);
        return fila;
    }

    /** Momento en que cada OT sin ejecutor volvió a quedar en espera (cola o devuelta). */
    private Map<UUID, Instant> ultimaEntradaACola(List<Orden> abiertas) {
        Map<UUID, Instant> entradas = new HashMap<>();
        List<UUID> sinEjecutor = abiertas.stream().filter(o -> o.getUsuario() == null).map(Orden::getId_orden).toList();
        if (sinEjecutor.isEmpty()) {
            return entradas;
        }
        for (AsignacionOrden evento : asignacionOrdenRepository.findByOrdenes(sinEjecutor)) {
            if (ENTRA_A_COLA.contains(evento.getTipo())) {
                entradas.put(evento.getOrden().getId_orden(), evento.getFecha());
            }
        }
        return entradas;
    }
}
