package cfbd.co.sgt.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cfbd.co.sgt.dto.response.KpiResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.UsuarioEspecialidad;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.repository.UsuarioEspecialidadRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.AutorizacionRecursoService;
import cfbd.co.sgt.service.KpiService;
import cfbd.co.sgt.service.kpi.AlcanceKpi;
import cfbd.co.sgt.service.kpi.KpiCalculator;
import cfbd.co.sgt.service.kpi.MetasKpi;

@Service
public class KpiServiceImpl implements KpiService {

    private static final long DIAS_POR_DEFECTO = 30;
    private static final long DIAS_MAXIMOS = 366;
    private static final String ALCANCE_GLOBAL = "Global";

    private final Map<String, KpiCalculator> calculadores;
    private final AutorizacionRecursoService autorizacion;
    private final UsuarioActualProvider usuarioActual;
    private final UsuarioEspecialidadRepository usuarioEspecialidadRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ZoneId zona;
    private final MetasKpi metas;

    public KpiServiceImpl(List<KpiCalculator> calculadores, AutorizacionRecursoService autorizacion,
                          UsuarioActualProvider usuarioActual, UsuarioEspecialidadRepository usuarioEspecialidadRepository,
                          EspecialidadRepository especialidadRepository,
                          @Value("${app.kpi.zona-horaria:America/Lima}") String zonaHoraria, MetasKpi metas) {
        this.metas = metas;
        this.calculadores = calculadores.stream().collect(Collectors.toMap(KpiCalculator::familia, Function.identity()));
        this.autorizacion = autorizacion;
        this.usuarioActual = usuarioActual;
        this.usuarioEspecialidadRepository = usuarioEspecialidadRepository;
        this.especialidadRepository = especialidadRepository;
        this.zona = ZoneId.of(zonaHoraria);
    }

    @Override
    @Transactional(readOnly = true)
    public KpiResponse calcular(String familia, LocalDate desde, LocalDate hasta, UUID idEspecialidad) {
        KpiCalculator calculador = calculadores.get(familia);
        if (calculador == null) {
            throw new ResourceNotFoundException("KPI '" + familia + "' no existe.");
        }
        LocalDate fin = hasta != null ? hasta : LocalDate.now(zona);
        LocalDate inicio = desde != null ? desde : fin.minusDays(DIAS_POR_DEFECTO);
        if (inicio.isAfter(fin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > DIAS_MAXIMOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango no puede superar " + DIAS_MAXIMOS + " días.");
        }
        Instant desdeInstante = inicio.atStartOfDay(zona).toInstant();
        Instant hastaInstante = fin.plusDays(1).atStartOfDay(zona).toInstant();
        return metas.aplicar(calculador.calcular(alcance(desdeInstante, hastaInstante, idEspecialidad)));
    }

    /**
     * Alcance global con orden.read_all (Admin, Despachador); si no, solo las
     * especialidades de las que el actor es responsable (FR-037).
     */
    private AlcanceKpi alcance(Instant desde, Instant hasta, UUID idEspecialidad) {
        if (autorizacion.veTodasLasOrdenes()) {
            if (idEspecialidad == null) {
                return new AlcanceKpi(desde, hasta, null, List.of(ALCANCE_GLOBAL));
            }
            String nombre = especialidadRepository.findById(idEspecialidad)
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found")).getNombre();
            return new AlcanceKpi(desde, hasta, List.of(idEspecialidad), List.of(nombre));
        }
        List<UsuarioEspecialidad> comoResponsable = usuarioEspecialidadRepository
                .findByUsuario(usuarioActual.obtener().getId_usuario()).stream()
                .filter(pertenencia -> Boolean.TRUE.equals(pertenencia.getEs_responsable()))
                .filter(pertenencia -> idEspecialidad == null
                        || pertenencia.getEspecialidad().getId_especialidad().equals(idEspecialidad))
                .toList();
        if (comoResponsable.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puede ver KPIs de las especialidades de las que es responsable.");
        }
        return new AlcanceKpi(desde, hasta,
                comoResponsable.stream().map(p -> p.getEspecialidad().getId_especialidad()).toList(),
                comoResponsable.stream().map(p -> p.getEspecialidad().getNombre()).toList());
    }
}
