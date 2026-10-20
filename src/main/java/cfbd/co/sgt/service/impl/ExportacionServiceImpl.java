package cfbd.co.sgt.service.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.dto.response.RequerimientoResponse;
import cfbd.co.sgt.model.Activo;
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.repository.ActivoRepository;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.RequerimientoRepository;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.service.CsvExporter;
import cfbd.co.sgt.service.ExportacionService;
import cfbd.co.sgt.service.OrdenService;
import cfbd.co.sgt.service.RequerimientoService;
import cfbd.co.sgt.service.SolicitudService;

@Service
@Transactional(readOnly = true)
public class ExportacionServiceImpl implements ExportacionService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SolicitudService solicitudService;
    private final RequerimientoService requerimientoService;
    private final OrdenService ordenService;
    private final EstadoRepository estadoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ActivoRepository activoRepository;
    private final SolicitudRepository solicitudRepository;
    private final RequerimientoRepository requerimientoRepository;
    private final CsvExporter csvExporter;
    private final ZoneId zona;

    public ExportacionServiceImpl(SolicitudService solicitudService, RequerimientoService requerimientoService,
                                  OrdenService ordenService, EstadoRepository estadoRepository,
                                  EspecialidadRepository especialidadRepository, ActivoRepository activoRepository,
                                  SolicitudRepository solicitudRepository,
                                  RequerimientoRepository requerimientoRepository, CsvExporter csvExporter,
                                  @Value("${app.kpi.zona-horaria:America/Lima}") String zonaHoraria) {
        this.solicitudService = solicitudService;
        this.requerimientoService = requerimientoService;
        this.ordenService = ordenService;
        this.estadoRepository = estadoRepository;
        this.especialidadRepository = especialidadRepository;
        this.activoRepository = activoRepository;
        this.solicitudRepository = solicitudRepository;
        this.requerimientoRepository = requerimientoRepository;
        this.csvExporter = csvExporter;
        this.zona = ZoneId.of(zonaHoraria);
    }

    @Override
    public String exportarSolicitudes() {
        Map<UUID, String> estados = estados();
        Map<UUID, String> especialidades = especialidades();
        Map<UUID, String> activos = indice(activoRepository.findAll(), Activo::getId_activo, Activo::getNombre);
        List<List<Object>> filas = solicitudService.listarSolicitudes().stream()
                .map(solicitud -> Arrays.<Object>asList(
                        solicitud.getNumeroSolicitud(),
                        fecha(solicitud.getFecha_registro()),
                        solicitud.getNombre_usuario(),
                        activos.get(solicitud.getId_activo()),
                        especialidades.get(solicitud.getId_especialidad()),
                        solicitud.getPrioridad(),
                        estados.get(solicitud.getId_estado()),
                        fecha(solicitud.getFecha_limite_despacho()),
                        solicitud.getDescripcion()))
                .toList();
        return csvExporter.exportar(List.of("Número", "Fecha de registro", "Solicitante", "Activo", "Especialidad",
                "Prioridad", "Estado", "Límite de despacho", "Descripción"), filas);
    }

    @Override
    public String exportarRequerimientos() {
        Map<UUID, String> estados = estados();
        Map<UUID, String> especialidades = especialidades();
        List<RequerimientoResponse> requerimientos = requerimientoService.listarRequerimientos();
        Map<UUID, String> solicitudes = numerosSolicitud(
                requerimientos.stream().map(RequerimientoResponse::getId_solicitud).toList());
        List<List<Object>> filas = requerimientos.stream()
                .map(requerimiento -> Arrays.<Object>asList(
                        requerimiento.getNumeroRequerimiento(),
                        fecha(requerimiento.getFecha_registro()),
                        requerimiento.getNombre_usuario(),
                        especialidades.get(requerimiento.getId_especialidad()),
                        estados.get(requerimiento.getId_estado()),
                        solicitudes.get(requerimiento.getId_solicitud()),
                        fecha(requerimiento.getFecha_limite_despacho()),
                        requerimiento.getDescripcion()))
                .toList();
        return csvExporter.exportar(List.of("Número", "Fecha de registro", "Creado por", "Especialidad", "Estado",
                "Solicitud de origen", "Límite de decisión", "Descripción"), filas);
    }

    @Override
    public String exportarOrdenes() {
        Map<UUID, String> estados = estados();
        Map<UUID, String> especialidades = especialidades();
        List<OrdenResponse> ordenes = ordenService.listarOrdenes();
        Map<UUID, String> solicitudes = numerosSolicitud(ordenes.stream().map(OrdenResponse::getId_solicitud).toList());
        Map<UUID, String> requerimientos = indice(requerimientoRepository.findAllById(
                        ordenes.stream().map(OrdenResponse::getId_requerimiento).filter(Objects::nonNull).distinct().toList()),
                Requerimiento::getId_requerimiento, Requerimiento::getNumeroRequerimiento);
        List<List<Object>> filas = ordenes.stream()
                .map(orden -> Arrays.<Object>asList(
                        orden.getNumeroOrden(),
                        fecha(orden.getFecha_registro()),
                        especialidades.get(orden.getId_especialidad()),
                        estados.get(orden.getId_estado()),
                        orden.getNombre_ejecutor(),
                        orden.getId_solicitud() != null ? solicitudes.get(orden.getId_solicitud())
                                : requerimientos.get(orden.getId_requerimiento()),
                        fecha(orden.getFecha_cierre())))
                .toList();
        return csvExporter.exportar(List.of("Número", "Fecha de registro", "Especialidad", "Estado", "Ejecutor",
                "Origen", "Fecha de cierre"), filas);
    }

    private Map<UUID, String> estados() {
        return indice(estadoRepository.findAll(), Estado::getId_estado, Estado::getNombre);
    }

    private Map<UUID, String> especialidades() {
        return indice(especialidadRepository.findAll(), Especialidad::getId_especialidad, Especialidad::getNombre);
    }

    private Map<UUID, String> numerosSolicitud(Collection<UUID> ids) {
        return indice(solicitudRepository.findAllById(ids.stream().filter(Objects::nonNull).distinct().toList()),
                Solicitud::getId_solicitud, Solicitud::getNumeroSolicitud);
    }

    private static <T> Map<UUID, String> indice(List<T> entidades, Function<T, UUID> id, Function<T, String> nombre) {
        Map<UUID, String> resultado = new HashMap<>();
        entidades.forEach(entidad -> resultado.put(id.apply(entidad), nombre.apply(entidad)));
        return resultado;
    }

    private String fecha(Instant instante) {
        return instante == null ? null : FORMATO_FECHA.format(instante.atZone(zona));
    }
}
