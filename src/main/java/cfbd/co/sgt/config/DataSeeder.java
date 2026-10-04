package cfbd.co.sgt.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cfbd.co.sgt.dto.request.UsuarioRequest;
import cfbd.co.sgt.exception.DuplicateResourceException;
import cfbd.co.sgt.model.Activo;
import cfbd.co.sgt.model.ActivoEspecialidad;
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.Permiso;
import cfbd.co.sgt.model.Rol;
import cfbd.co.sgt.model.RolPermiso;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.model.UsuarioEspecialidad;
import cfbd.co.sgt.repository.ActivoEspecialidadRepository;
import cfbd.co.sgt.repository.ActivoRepository;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.RolPermisoRepository;
import cfbd.co.sgt.repository.UsuarioEspecialidadRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.service.PermisoService;
import cfbd.co.sgt.service.RolService;
import cfbd.co.sgt.service.UsuarioService;
import lombok.RequiredArgsConstructor;

/**
 * Carga los datos iniciales del sistema (permisos, roles, usuarios de prueba, especialidades, activos y estados). Idempotente: se apoya en
 * campos naturales (codigo/nombre/email) para no duplicar registros en reinicios sucesivos de la aplicación.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final List<String> RECURSOS_CON_CRUD = List.of(
            "usuario", "rol", "solicitud", "requerimiento", "orden",
            "activo", "especialidad", "estado", "permiso");
    private static final List<String> ACCIONES_CRUD = List.of("read", "create", "update", "delete");

    // Recursos que solo exponen un subconjunto de acciones (ver decisión de alcance del CRUD: auditoría/eventos de negocio no se editan ni se borran,
    // y rolpermiso no tiene "update" porque una asignación se revoca y se
    // vuelve a crear, no se edita). "aprobacion" perdió "create": esa
    // operación ahora está gobernada por el permiso de negocio
    // requerimiento.aprobar (ver PERMISOS_DE_NEGOCIO), no por un CRUD genérico.
    private static final Map<String, List<String>> RECURSOS_CON_ACCIONES_PARCIALES = Map.of(
            "aprobacion", List.of("read"),
            "derivacion", List.of("read", "create"),
            "rolpermiso", List.of("read", "create", "delete"),
            "historial_orden", List.of("read", "create"),
            "historial_requerimiento", List.of("read", "create"),
            "historial_solicitud", List.of("read", "create"));

    // Permisos de negocio: no son un par <recurso>.<acción CRUD>, sino una
    // operación diferenciada que requiere su propia autorización (CLAUDE.md
    // 5.3). orden.create deliberadamente NO se agrega aquí ni se asigna a
    // ningún rol: generar una Orden solo puede ocurrir a través de
    // solicitud.generar_orden o requerimiento.generar_orden, nunca por CRUD
    // genérico (evita saltarse el flujo).
    private static final List<String> PERMISOS_DE_NEGOCIO = List.of(
            "solicitud.generar_orden",
            "solicitud.generar_requerimiento",
            "requerimiento.generar_orden",
            "requerimiento.aprobar",
            "orden.cerrar",
            "orden.reasignar",
            // Alcance de lectura (CLAUDE.md 6.5): sin ellos solo se ve lo propio.
            "solicitud.read_all",
            "requerimiento.read_all",
            "orden.read_all",
            // Cola de OT por especialidad (PRD E3) — autorizados 2026-10-03.
            "orden.tomar",
            "orden.asignar",
            "orden.verificar",
            "especialidad.gestionar_equipo",
            // KPIs (PRD E6).
            "kpi.read",
            "kpi.export");

    private final RolService rolService;
    private final PermisoService permisoService;
    private final RolPermisoRepository rolPermisoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final EstadoRepository estadoRepository;
    private final ActivoRepository activoRepository;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEspecialidadRepository usuarioEspecialidadRepository;
    private final ActivoEspecialidadRepository activoEspecialidadRepository;

    @Value("${app.seed.default-password:ChangeMe123!}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Rol> roles = seedRoles();
        Map<String, Permiso> permisos = seedPermisos();
        seedRolPermisos(roles, permisos);
        Map<String, Especialidad> especialidades = seedEspecialidades();
        seedEstados();
        seedActivos(especialidades);
        seedUsuarios(roles);
        seedEquipos(especialidades);
        log.info("DataSeeder: carga de datos iniciales verificada (permisos={}, roles={}).",
                permisos.size(), roles.size());
    }

    private Map<String, Rol> seedRoles() {
        Map<String, Rol> resultado = new LinkedHashMap<>();
        for (String nombre : List.of("Cliente", "Despachador", "Administrador", "Operaciones")) {
            Rol rol = rolService.buscarRolPorNombre(nombre).orElseGet(() -> {
                Rol nuevo = new Rol();
                nuevo.setNombre(nombre);
                nuevo.setDescripcion("Rol " + nombre);
                log.info("DataSeeder: creando rol '{}'.", nombre);
                return rolService.crearRol(nuevo);
            });
            resultado.put(nombre, rol);
        }
        return resultado;
    }

    private Map<String, Permiso> seedPermisos() {
        Map<String, Permiso> resultado = new LinkedHashMap<>();
        for (String recurso : RECURSOS_CON_CRUD) {
            for (String accion : ACCIONES_CRUD) {
                seedPermiso(resultado, recurso, accion);
            }
        }
        RECURSOS_CON_ACCIONES_PARCIALES.forEach((recurso, acciones) -> {
            for (String accion : acciones) {
                seedPermiso(resultado, recurso, accion);
            }
        });
        for (String codigo : PERMISOS_DE_NEGOCIO) {
            int puntoSeparador = codigo.indexOf('.');
            seedPermiso(resultado, codigo.substring(0, puntoSeparador), codigo.substring(puntoSeparador + 1));
        }
        return resultado;
    }

    private void seedPermiso(Map<String, Permiso> resultado, String recurso, String accion) {
        String codigo = recurso + "." + accion;
        Permiso permiso = permisoService.buscarPermisoPorCodigo(codigo).orElseGet(() -> {
            Permiso nuevo = new Permiso();
            nuevo.setCodigo(codigo);
            nuevo.setDescripcion("Permite " + accion + " sobre " + recurso);
            log.info("DataSeeder: creando permiso '{}'.", codigo);
            return permisoService.crearPermiso(nuevo);
        });
        resultado.put(codigo, permiso);
    }

    /**
     * Matriz de permisos por rol, alineada con el flujo de negocio definitivo
     * (Cliente reporta; Despachador clasifica y decide bajo-contrato vs
     * Requerimiento; Administrador tiene control total y aprueba/rechaza;
     * Operaciones ejecuta y cierra Órdenes). orden.create NO se asigna a
     * ningún rol: generar una Orden solo puede ocurrir a través de
     * solicitud.generar_orden / requerimiento.generar_orden (ver
     * SolicitudServiceImpl/RequerimientoServiceImpl), nunca por el CRUD
     * genérico de Orden, para que nadie pueda saltarse el flujo. Los
     * historial_*.create tampoco se reparten por rol: las operaciones de
     * negocio (aprobar, generar OT, cerrar OT) registran su propio historial
     * internamente, con el usuario autenticado, dentro de la misma
     * transacción — no dependen de que el cliente llame al endpoint genérico
     * de historial por separado.
     */
    private void seedRolPermisos(Map<String, Rol> roles, Map<String, Permiso> permisos) {
        Map<String, List<String>> matriz = Map.of(
                // Cliente ya no tiene usuario.read (AUDITORIA S3): los nombres que
                // necesita llegan en SolicitudResponse/OrdenResponse.
                "Cliente", List.of(
                        "solicitud.read", "solicitud.create", "solicitud.update",
                        "activo.read", "estado.read", "especialidad.read",
                        "orden.read", "historial_orden.read", "historial_solicitud.read"),
                "Despachador", List.of(
                        "solicitud.read", "solicitud.update", "solicitud.generar_orden", "solicitud.generar_requerimiento",
                        "requerimiento.read", "requerimiento.create", "requerimiento.update",
                        "activo.read", "estado.read", "especialidad.read", "usuario.read",
                        "orden.read", "historial_orden.read",
                        "derivacion.read", "derivacion.create",
                        "historial_solicitud.read",
                        "solicitud.read_all", "requerimiento.read_all", "orden.read_all",
                        "kpi.read"),
                "Administrador", List.of(
                        "usuario.read", "usuario.create", "usuario.update", "usuario.delete",
                        "rol.read", "rol.create", "rol.update", "rol.delete",
                        "permiso.read", "permiso.create", "permiso.update", "permiso.delete",
                        "rolpermiso.read", "rolpermiso.create", "rolpermiso.delete",
                        "activo.read", "activo.create", "activo.update", "activo.delete",
                        "especialidad.read", "especialidad.create", "especialidad.update", "especialidad.delete",
                        "estado.read", "estado.create", "estado.update", "estado.delete",
                        "solicitud.read", "solicitud.create", "solicitud.update", "solicitud.delete",
                        "solicitud.generar_orden", "solicitud.generar_requerimiento",
                        "requerimiento.read", "requerimiento.create", "requerimiento.update", "requerimiento.delete",
                        "requerimiento.aprobar", "requerimiento.generar_orden",
                        "orden.read", "orden.update", "orden.delete", "orden.reasignar",
                        "derivacion.read",
                        "aprobacion.read",
                        "historial_orden.read", "historial_requerimiento.read", "historial_solicitud.read",
                        "solicitud.read_all", "requerimiento.read_all", "orden.read_all",
                        "especialidad.gestionar_equipo", "kpi.read", "kpi.export"),
                "Operaciones", List.of(
                        "solicitud.read", "solicitud.create", "solicitud.update",
                        "requerimiento.read", "requerimiento.create", "requerimiento.update",
                        "orden.read", "orden.update", "orden.cerrar", "orden.reasignar",
                        "orden.tomar", "orden.verificar", "orden.asignar", "kpi.read",
                        "activo.read", "estado.read",
                        "especialidad.read",
                        "historial_orden.read", "historial_requerimiento.read", "historial_solicitud.read")
        );

        matriz.forEach((nombreRol, codigosPermiso) -> {
            Rol rol = roles.get(nombreRol);
            for (String codigo : codigosPermiso) {
                Permiso permiso = permisos.get(codigo);
                if (permiso == null) {
                    continue;
                }
                if (!rolPermisoRepository.existsByRolAndPermiso(rol, permiso)) {
                    RolPermiso rolPermiso = new RolPermiso();
                    rolPermiso.setRol(rol);
                    rolPermiso.setPermiso(permiso);
                    rolPermisoRepository.save(rolPermiso);
                    log.info("DataSeeder: asignando permiso '{}' al rol '{}'.", codigo, nombreRol);
                }
            }
        });
    }

    /** Catálogo vigente de especialidades (PRD D4): "Soporte" se divide en
     * dos. Las OT se autoasignan a la cola de una de estas especialidades. */
    private Map<String, Especialidad> seedEspecialidades() {
        record EspecialidadSeed(String nombre, String descripcion) {
        }
        List<EspecialidadSeed> especialidades = List.of(
                new EspecialidadSeed("Desarrollo", "Desarrollo de software"),
                new EspecialidadSeed("Implementación", "Implementación de soluciones"),
                new EspecialidadSeed("DevOPS", "Infraestructura, CI/CD y operaciones"),
                new EspecialidadSeed("Soporte y mantenimiento de código",
                        "Soporte y mantenimiento de código de las soluciones"),
                new EspecialidadSeed("Soporte de infraestructura y configuración de analíticas",
                        "Soporte de infraestructura y configuración de analíticas"));

        Map<String, Especialidad> resultado = new LinkedHashMap<>();
        for (EspecialidadSeed seed : especialidades) {
            Especialidad especialidad = especialidadRepository.findByNombre(seed.nombre()).orElseGet(() -> {
                Especialidad nueva = new Especialidad();
                nueva.setNombre(seed.nombre());
                nueva.setDescripcion(seed.descripcion());
                log.info("DataSeeder: creando especialidad '{}'.", seed.nombre());
                return especialidadRepository.save(nueva);
            });
            resultado.put(seed.nombre(), especialidad);
        }
        return resultado;
    }

    private void seedEstados() {
        for (String nombre : List.of("Pendiente", "En revisión", "En progreso", "Finalizado", "Aprobado", "Rechazado",
                "Asignada", "Devuelta")) {
            if (estadoRepository.findByNombre(nombre).isEmpty()) {
                Estado nuevo = new Estado();
                nuevo.setNombre(nombre);
                estadoRepository.save(nuevo);
                log.info("DataSeeder: creando estado '{}'.", nombre);
            }
        }
    }

    /** Activos y sus especialidades (decisión 2026-10-04): la primera de la
     * lista es la principal (activo.id_especialidad, con la que nace la
     * Solicitud); todas quedan en activo_especialidad. Idempotente: un activo
     * existente sin especialidades adicionales las recibe. */
    private void seedActivos(Map<String, Especialidad> especialidades) {
        record ActivoSeed(String nombre, String codigo, List<String> especialidades) {
        }
        String soporteCodigo = "Soporte y mantenimiento de código";
        String soporteInfra = "Soporte de infraestructura y configuración de analíticas";
        List<ActivoSeed> activos = List.of(
                new ActivoSeed("Azor Panel", "AZR-PANEL", List.of(soporteCodigo, "Desarrollo", "DevOPS")),
                new ActivoSeed("Azor Analytics", "AZR-ANALYTICS", List.of(soporteCodigo, "Desarrollo", "DevOPS")),
                new ActivoSeed("Network Optix", "NX-VMS", List.of(soporteInfra, "Implementación")),
                // Hardware (servidores): solo Soporte de infraestructura.
                new ActivoSeed("Servidor 1", "SRV-01", List.of(soporteInfra)));

        for (ActivoSeed seed : activos) {
            Activo activo = activoRepository.findByNombre(seed.nombre()).orElseGet(() -> {
                Activo nuevo = new Activo();
                nuevo.setEspecialidad(especialidades.get(seed.especialidades().get(0)));
                nuevo.setCodigo(seed.codigo());
                nuevo.setNombre(seed.nombre());
                log.info("DataSeeder: creando activo '{}'.", seed.nombre());
                return activoRepository.save(nuevo);
            });
            if (activoEspecialidadRepository.findIdsEspecialidad(activo.getId_activo()).isEmpty()) {
                for (String nombreEspecialidad : seed.especialidades()) {
                    ActivoEspecialidad relacion = new ActivoEspecialidad();
                    relacion.setActivo(activo);
                    relacion.setEspecialidad(especialidades.get(nombreEspecialidad));
                    activoEspecialidadRepository.save(relacion);
                }
            }
        }
    }

    private void seedUsuarios(Map<String, Rol> roles) {
        record UsuarioSeed(String email, String nombres, String apellidos, String rol) {
        }
        List<UsuarioSeed> usuarios = List.of(
                new UsuarioSeed("cliente1@cfbd.co", "Cliente", "Uno", "Cliente"),
                new UsuarioSeed("despachador1@cfbd.co", "Despachador", "Uno", "Despachador"),
                new UsuarioSeed("administrador1@cfbd.co", "Administrador", "Uno", "Administrador"),
                new UsuarioSeed("operaciones1@cfbd.co", "Operaciones", "Uno", "Operaciones"),
                new UsuarioSeed("yortiz@cfbd.co", "Yersy Saul", "Ortiz Mallqui", "Operaciones"),
                new UsuarioSeed("ddiaz@cfbd.co", "Danny", "Diaz Cordova", "Operaciones"),
                new UsuarioSeed("carlos@cfbd.co", "Carlos", "Barrientos Diliberto", "Administrador"),
                new UsuarioSeed("pgaspar@cfbd.co", "Pedro", "Gaspar Ortiz", "Operaciones"),
                new UsuarioSeed("mjimenez@cfbd.co", "Miguel", "Jimenez", "Operaciones"),
                new UsuarioSeed("aperalta@cfbd.co", "Alicia", "Peralta", "Operaciones"),
                new UsuarioSeed("lulloa@cfbd.co", "Lorenzo", "Ulloa Alva", "Operaciones"),
                new UsuarioSeed("rjuarez@cfbd.co", "Ricardo", "Juares Blaz", "Operaciones"),
                new UsuarioSeed("olopez@cfbd.co", "Oscar", "Lopez", "Operaciones"),
                new UsuarioSeed("ldesposorio@cfbd.co", "Cristofer", "Geronimo", "Operaciones"));
                
        for (UsuarioSeed seed : usuarios) {
            if (usuarioRepository.existsByEmail(seed.email())) {
                continue;
            }
            UsuarioRequest request = new UsuarioRequest();
            request.setEmail(seed.email());
            request.setPassword(defaultPassword);
            request.setNombres(seed.nombres());
            request.setApellidos(seed.apellidos());
            request.setId_rol(roles.get(seed.rol()).getId_rol());
            try {
                usuarioService.crearUsuario(request);
                log.info("DataSeeder: creando usuario '{}' con rol '{}'.", seed.email(), seed.rol());
            } catch (DuplicateResourceException ex) {
                log.debug("DataSeeder: usuario '{}' ya existe, se omite.", seed.email());
            }
        }
    }

    /**
     * Equipos por especialidad definidos por el dueño del producto (PRD OQ-10,
     * 2026-10-04). Solo se siembra el equipo de una especialidad que aún no
     * tiene miembros: los cambios hechos luego desde Administración no se
     * pisan en cada arranque. Un email sin usuario se omite con WARN.
     */
    private void seedEquipos(Map<String, Especialidad> especialidades) {
        record MiembroSeed(String email, boolean responsable) {
        }
        Map<String, List<MiembroSeed>> equipos = new LinkedHashMap<>();
        equipos.put("Desarrollo", List.of(
                new MiembroSeed("yortiz@cfbd.co", true),
                new MiembroSeed("lulloa@cfbd.co", false),
                new MiembroSeed("aperalta@cfbd.co", false)));
        equipos.put("Soporte y mantenimiento de código", List.of(
                new MiembroSeed("yortiz@cfbd.co", true),
                new MiembroSeed("lulloa@cfbd.co", false),
                new MiembroSeed("aperalta@cfbd.co", false)));
        equipos.put("Implementación", List.of(
                new MiembroSeed("pgaspar@cfbd.co", true),
                new MiembroSeed("ddiaz@cfbd.co", false),
                new MiembroSeed("mjimenez@cfbd.co", false)));
        equipos.put("DevOPS", List.of(
                new MiembroSeed("pgaspar@cfbd.co", true),
                new MiembroSeed("ddiaz@cfbd.co", false)));
        equipos.put("Soporte de infraestructura y configuración de analíticas", List.of(
                new MiembroSeed("pgaspar@cfbd.co", true),
                new MiembroSeed("mjimenez@cfbd.co", false),
                new MiembroSeed("rjuarez@cfbd.co", false),
                new MiembroSeed("olopez@cfbd.co", false),
                new MiembroSeed("ldesposorio@cfbd.co", false)));

        equipos.forEach((nombreEspecialidad, miembros) -> {
            Especialidad especialidad = especialidades.get(nombreEspecialidad);
            if (especialidad == null
                    || !usuarioEspecialidadRepository.findByEspecialidad(especialidad.getId_especialidad()).isEmpty()) {
                return;
            }
            for (MiembroSeed miembro : miembros) {
                Usuario usuario = usuarioRepository.findByEmail(miembro.email()).orElse(null);
                if (usuario == null) {
                    log.warn("DataSeeder: '{}' no existe; no se agrega al equipo de '{}'.", miembro.email(), nombreEspecialidad);
                    continue;
                }
                UsuarioEspecialidad pertenencia = new UsuarioEspecialidad();
                pertenencia.setUsuario(usuario);
                pertenencia.setEspecialidad(especialidad);
                pertenencia.setEs_responsable(miembro.responsable());
                usuarioEspecialidadRepository.save(pertenencia);
            }
            log.info("DataSeeder: equipo de '{}' sembrado.", nombreEspecialidad);
        });
    }
}
