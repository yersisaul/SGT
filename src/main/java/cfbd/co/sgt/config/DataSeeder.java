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
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.Permiso;
import cfbd.co.sgt.model.Rol;
import cfbd.co.sgt.model.RolPermiso;
import cfbd.co.sgt.repository.ActivoRepository;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.RolPermisoRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.service.PermisoService;
import cfbd.co.sgt.service.RolService;
import cfbd.co.sgt.service.UsuarioService;
import lombok.RequiredArgsConstructor;

/**
 * Carga los datos iniciales del sistema (permisos, roles, usuarios de
 * prueba, especialidades, activos y estados). Idempotente: se apoya en
 * campos naturales (codigo/nombre/email) para no duplicar registros en
 * reinicios sucesivos de la aplicación.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final List<String> RECURSOS_CON_CRUD = List.of(
            "usuario", "rol", "solicitud", "requerimiento", "orden",
            "activo", "especialidad", "estado", "permiso");
    private static final List<String> ACCIONES_CRUD = List.of("read", "create", "update", "delete");

    // Recursos que solo exponen un subconjunto de acciones (ver decisión de
    // alcance del CRUD: auditoría/eventos de negocio no se editan ni se borran,
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
            "orden.reasignar");

    private final RolService rolService;
    private final PermisoService permisoService;
    private final RolPermisoRepository rolPermisoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final EstadoRepository estadoRepository;
    private final ActivoRepository activoRepository;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

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
        seedActivos(especialidades.get("Videovigilancia"));
        seedUsuarios(roles);
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
                "Cliente", List.of(
                        "solicitud.read", "solicitud.create",
                        "activo.read", "estado.read", "especialidad.read", "usuario.read"),
                "Despachador", List.of(
                        "solicitud.read", "solicitud.update", "solicitud.generar_orden", "solicitud.generar_requerimiento",
                        "requerimiento.read", "requerimiento.create", "requerimiento.update",
                        "activo.read", "estado.read", "especialidad.read", "usuario.read",
                        "derivacion.read", "derivacion.create",
                        "historial_solicitud.read"),
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
                        "historial_orden.read", "historial_requerimiento.read", "historial_solicitud.read"),
                "Operaciones", List.of(
                        "solicitud.read",
                        "requerimiento.read",
                        "orden.read", "orden.update", "orden.cerrar", "orden.reasignar",
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

    /** "Videovigilancia" se conserva (los Activos sembrados por seedActivos
     * la referencian); las 4 siguientes son las especialidades pedidas para
     * clasificar trabajo interno de desarrollo. */
    private Map<String, Especialidad> seedEspecialidades() {
        record EspecialidadSeed(String nombre, String descripcion) {
        }
        List<EspecialidadSeed> especialidades = List.of(
                new EspecialidadSeed("Videovigilancia", "Sistemas y software de videovigilancia y monitoreo"),
                new EspecialidadSeed("Desarrollo", "Desarrollo de software"),
                new EspecialidadSeed("Implementación", "Implementación de soluciones"),
                new EspecialidadSeed("DevOPS", "Infraestructura, CI/CD y operaciones"),
                new EspecialidadSeed("Soporte", "Soporte técnico y mantenimiento"));

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
        for (String nombre : List.of("Pendiente", "En revisión", "En progreso", "Finalizado", "Aprobado", "Rechazado")) {
            if (estadoRepository.findByNombre(nombre).isEmpty()) {
                Estado nuevo = new Estado();
                nuevo.setNombre(nombre);
                estadoRepository.save(nuevo);
                log.info("DataSeeder: creando estado '{}'.", nombre);
            }
        }
    }

    private void seedActivos(Especialidad especialidad) {
        record ActivoSeed(String nombre, String codigo) {
        }
        List<ActivoSeed> activos = List.of(
                new ActivoSeed("Azor Panel", "AZR-PANEL"),
                new ActivoSeed("Azor Analytics", "AZR-ANALYTICS"),
                new ActivoSeed("Network Optix", "NX-VMS"),
                new ActivoSeed("Servidor 1", "SRV-01"));

        for (ActivoSeed seed : activos) {
            if (activoRepository.findByNombre(seed.nombre()).isEmpty()) {
                Activo nuevo = new Activo();
                nuevo.setEspecialidad(especialidad);
                nuevo.setCodigo(seed.codigo());
                nuevo.setNombre(seed.nombre());
                activoRepository.save(nuevo);
                log.info("DataSeeder: creando activo '{}'.", seed.nombre());
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
                new UsuarioSeed("carlos@cfbd.co", "Carlos", "Barrientos Diliberto", "Administrador"));
                
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
}
