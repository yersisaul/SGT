package cfbd.co.sgt.service.kpi;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.KpiColumnaResponse;
import cfbd.co.sgt.dto.response.KpiIndicadorResponse;
import cfbd.co.sgt.dto.response.KpiResponse;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.repository.RequerimientoRepository;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.service.SlaCalculator;
import lombok.RequiredArgsConstructor;

/**
 * SLA de atención (decisión 2026-10-04): % de Solicitudes bajo contrato
 * cuya OT se cerró antes de registro + SLA de atención (Alta 8 h, Media
 * 24 h, Baja 72 h). Las que fueron fuera de contrato (generaron RQ) se
 * excluyen: dependen de la aprobación. Sin cerrar y vencida = fuera de SLA.
 */
@Component
@RequiredArgsConstructor
class SlaAtencionKpiCalculator implements KpiCalculator {

    private static final List<String> PRIORIDADES = List.of("Alta", "Media", "Baja");
    private static final String OTRA_PRIORIDAD = "Sin prioridad";

    private final SolicitudRepository solicitudRepository;
    private final OrdenRepository ordenRepository;
    private final RequerimientoRepository requerimientoRepository;
    private final SlaCalculator slaCalculator;

    @Override
    public String familia() {
        return "sla-atencion";
    }

    @Override
    public KpiResponse calcular(AlcanceKpi alcance) {
        List<Solicitud> solicitudes = alcance.esGlobal()
                ? solicitudRepository.findRegistradasEntre(alcance.desde(), alcance.hasta())
                : solicitudRepository.findRegistradasEntre(alcance.desde(), alcance.hasta(), alcance.idsEspecialidad());
        List<UUID> ids = solicitudes.stream().map(Solicitud::getId_solicitud).toList();
        Set<UUID> fueraDeContrato = ids.isEmpty() ? Set.of()
                : new HashSet<>(requerimientoRepository.findIdsSolicitudConRequerimiento(ids));
        Map<UUID, Instant> cierres = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Orden orden : ordenRepository.findBySolicitudes(ids)) {
                if (orden.getFecha_cierre() != null) {
                    cierres.put(orden.getSolicitud().getId_solicitud(), orden.getFecha_cierre());
                }
            }
        }
        Instant ahora = Instant.now();
        Map<String, ConteoSla> porPrioridad = new LinkedHashMap<>();
        PRIORIDADES.forEach(prioridad -> porPrioridad.put(prioridad, new ConteoSla()));
        ConteoSla total = new ConteoSla();
        for (Solicitud solicitud : solicitudes) {
            if (fueraDeContrato.contains(solicitud.getId_solicitud())) {
                continue;
            }
            Instant limite = slaCalculator.deadlineAtencion(solicitud.getFecha_registro(), solicitud.getPrioridad());
            Instant cierre = cierres.get(solicitud.getId_solicitud());
            porPrioridad.computeIfAbsent(prioridad(solicitud.getPrioridad()), p -> new ConteoSla())
                    .registrar(cierre, limite, ahora);
            total.registrar(cierre, limite, ahora);
        }
        List<Map<String, Object>> filas = porPrioridad.entrySet().stream()
                .map(entrada -> renombrar(entrada.getValue().fila(entrada.getKey())))
                .toList();
        return new KpiResponse(familia(), "SLA de atención", alcance.desde(), alcance.hasta(), alcance.nombres(), false,
                List.of(
                        KpiIndicadorResponse.de("cumplimiento", "Cumplimiento del SLA de atención", total.cumplimiento(), "%"),
                        KpiIndicadorResponse.de("bajo_contrato", "Solicitudes bajo contrato", (double) total.registradas, ""),
                        KpiIndicadorResponse.de("fuera_sla", "Fuera de SLA", (double) total.fuera, ""),
                        KpiIndicadorResponse.de("sin_cerrar", "Sin cerrar", (double) total.sinDespachar, "")),
                List.of(
                        new KpiColumnaResponse("prioridad", "Prioridad", ""),
                        new KpiColumnaResponse("registradas", "Bajo contrato", ""),
                        new KpiColumnaResponse("cerradas", "Cerradas", ""),
                        new KpiColumnaResponse("dentro_sla", "Dentro de SLA", ""),
                        new KpiColumnaResponse("fuera_sla", "Fuera de SLA", ""),
                        new KpiColumnaResponse("cumplimiento", "Cumplimiento", "%")),
                filas);
    }

    /** ConteoSla nombra "despachadas" al evento final; aquí es el cierre de la OT. */
    private Map<String, Object> renombrar(Map<String, Object> fila) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        fila.forEach((clave, valor) -> resultado.put("despachadas".equals(clave) ? "cerradas" : clave, valor));
        return resultado;
    }

    private String prioridad(String prioridad) {
        if (prioridad == null) {
            return OTRA_PRIORIDAD;
        }
        return PRIORIDADES.stream().filter(p -> p.equalsIgnoreCase(prioridad.trim())).findFirst().orElse(OTRA_PRIORIDAD);
    }
}
