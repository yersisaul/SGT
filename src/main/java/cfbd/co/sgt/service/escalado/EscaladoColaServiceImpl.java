package cfbd.co.sgt.service.escalado;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cfbd.co.sgt.dto.response.OrdenEscaladaResponse;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.TipoAsignacionOrden;
import cfbd.co.sgt.repository.AsignacionOrdenRepository;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.repository.UsuarioEspecialidadRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.AutorizacionRecursoService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class EscaladoColaServiceImpl implements EscaladoColaService {

    private static final Set<TipoAsignacionOrden> ENTRA_A_COLA = Set.of(TipoAsignacionOrden.ENCOLADA,
            TipoAsignacionOrden.REASIGNADA_ESPECIALIDAD);
    private static final String PRIORIDAD_POR_DEFECTO = "Media";

    private final OrdenRepository ordenRepository;
    private final AsignacionOrdenRepository asignacionOrdenRepository;
    private final UsuarioEspecialidadRepository usuarioEspecialidadRepository;
    private final AutorizacionRecursoService autorizacion;
    private final UsuarioActualProvider usuarioActual;
    private final UmbralesEscalado umbrales;

    @Override
    @Transactional(readOnly = true)
    public List<OrdenEscaladaResponse> detectar() {
        List<Orden> enCola = ordenRepository.findEnColaSinTomar();
        if (enCola.isEmpty()) {
            return List.of();
        }
        Map<UUID, Instant> entradas = new HashMap<>();
        for (Object[] fila : asignacionOrdenRepository.ultimaFechaPorTipo(
                enCola.stream().map(Orden::getId_orden).toList(), ENTRA_A_COLA)) {
            entradas.put((UUID) fila[0], (Instant) fila[1]);
        }
        Instant ahora = Instant.now();
        return enCola.stream()
                .map(orden -> evaluar(orden, entradas.getOrDefault(orden.getId_orden(), orden.getFecha_registro()), ahora))
                .filter(escalada -> escalada.nivel() > UmbralesEscalado.SIN_ESCALAR)
                .sorted(Comparator.comparingLong(OrdenEscaladaResponse::minutos_en_cola).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenEscaladaResponse> listarVisibles() {
        List<OrdenEscaladaResponse> todas = detectar();
        if (autorizacion.veTodasLasOrdenes()) {
            return todas;
        }
        Set<UUID> comoResponsable = usuarioEspecialidadRepository.findByUsuario(usuarioActual.obtener().getId_usuario())
                .stream()
                .filter(pertenencia -> Boolean.TRUE.equals(pertenencia.getEs_responsable()))
                .map(pertenencia -> pertenencia.getEspecialidad().getId_especialidad())
                .collect(Collectors.toSet());
        return todas.stream().filter(escalada -> comoResponsable.contains(escalada.id_especialidad())).toList();
    }

    private OrdenEscaladaResponse evaluar(Orden orden, Instant entradaCola, Instant ahora) {
        String prioridad = prioridad(orden);
        Duration enCola = Duration.between(entradaCola, ahora);
        return new OrdenEscaladaResponse(orden.getId_orden(), orden.getNumeroOrden(),
                orden.getEspecialidad().getId_especialidad(), orden.getEspecialidad().getNombre(), prioridad,
                entradaCola, enCola.toMinutes(), umbrales.nivel(enCola, prioridad));
    }

    /** La OT hereda la prioridad de su Solicitud (directa o vía RQ); un RQ sin Solicitud cuenta como Media. */
    private String prioridad(Orden orden) {
        if (orden.getSolicitud() != null) {
            return orden.getSolicitud().getPrioridad();
        }
        if (orden.getRequerimiento() != null && orden.getRequerimiento().getSolicitud() != null) {
            return orden.getRequerimiento().getSolicitud().getPrioridad();
        }
        return PRIORIDAD_POR_DEFECTO;
    }
}
