package cfbd.co.sgt.service.kpi;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.KpiColumnaResponse;
import cfbd.co.sgt.dto.response.KpiIndicadorResponse;
import cfbd.co.sgt.dto.response.KpiResponse;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.repository.HistorialSolicitudRepository;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.service.SlaCalculator;
import lombok.RequiredArgsConstructor;

/**
 * FR-030: % de Solicitudes despachadas (OT o RQ) antes de su
 * fecha_limite_despacho, por prioridad. Una Solicitud aún sin despachar y
 * ya vencida cuenta como fuera de SLA; una sin despachar dentro de plazo no
 * cuenta todavía.
 */
@Component
@RequiredArgsConstructor
class SlaDespachoKpiCalculator implements KpiCalculator {

    private static final List<String> PRIORIDADES = List.of("Alta", "Media", "Baja");
    private static final String OTRA_PRIORIDAD = "Sin prioridad";

    private final SolicitudRepository solicitudRepository;
    private final HistorialSolicitudRepository historialSolicitudRepository;
    private final SlaCalculator slaCalculator;

    @Override
    public String familia() {
        return "sla-despacho";
    }

    @Override
    public KpiResponse calcular(AlcanceKpi alcance) {
        List<Solicitud> solicitudes = alcance.esGlobal()
                ? solicitudRepository.findRegistradasEntre(alcance.desde(), alcance.hasta())
                : solicitudRepository.findRegistradasEntre(alcance.desde(), alcance.hasta(), alcance.idsEspecialidad());
        Map<UUID, Instant> despachos = primerDespacho(solicitudes);
        Instant ahora = Instant.now();

        Map<String, ConteoSla> porPrioridad = new LinkedHashMap<>();
        PRIORIDADES.forEach(prioridad -> porPrioridad.put(prioridad, new ConteoSla()));
        ConteoSla total = new ConteoSla();
        for (Solicitud solicitud : solicitudes) {
            Instant limite = slaCalculator.deadlineSolicitud(solicitud.getFecha_registro(), solicitud.getPrioridad());
            Instant despacho = despachos.get(solicitud.getId_solicitud());
            String prioridad = normalizarPrioridad(solicitud.getPrioridad());
            porPrioridad.computeIfAbsent(prioridad, clave -> new ConteoSla()).registrar(despacho, limite, ahora);
            total.registrar(despacho, limite, ahora);
        }

        List<Map<String, Object>> filas = porPrioridad.entrySet().stream()
                .map(entrada -> entrada.getValue().fila(entrada.getKey()))
                .toList();
        return new KpiResponse(familia(), "SLA de despacho", alcance.desde(), alcance.hasta(), alcance.nombres(), false,
                List.of(
                        KpiIndicadorResponse.de("cumplimiento", "Cumplimiento del SLA", total.cumplimiento(), "%"),
                        KpiIndicadorResponse.de("registradas", "Solicitudes registradas", (double) total.registradas, ""),
                        KpiIndicadorResponse.de("fuera_sla", "Fuera de SLA", (double) total.fuera, ""),
                        KpiIndicadorResponse.de("sin_despachar", "Sin despachar", (double) total.sinDespachar, "")),
                List.of(
                        new KpiColumnaResponse("prioridad", "Prioridad", ""),
                        new KpiColumnaResponse("registradas", "Registradas", ""),
                        new KpiColumnaResponse("despachadas", "Despachadas", ""),
                        new KpiColumnaResponse("dentro_sla", "Dentro de SLA", ""),
                        new KpiColumnaResponse("fuera_sla", "Fuera de SLA", ""),
                        new KpiColumnaResponse("cumplimiento", "Cumplimiento", "%")),
                filas);
    }

    private Map<UUID, Instant> primerDespacho(List<Solicitud> solicitudes) {
        Map<UUID, Instant> despachos = new HashMap<>();
        if (solicitudes.isEmpty()) {
            return despachos;
        }
        List<UUID> ids = solicitudes.stream().map(Solicitud::getId_solicitud).toList();
        for (Object[] fila : historialSolicitudRepository.primerDespacho(ids)) {
            despachos.put((UUID) fila[0], (Instant) fila[1]);
        }
        return despachos;
    }

    private String normalizarPrioridad(String prioridad) {
        if (prioridad == null) {
            return OTRA_PRIORIDAD;
        }
        return PRIORIDADES.stream().filter(p -> p.equalsIgnoreCase(prioridad.trim())).findFirst().orElse(OTRA_PRIORIDAD);
    }
}
