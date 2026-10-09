package cfbd.co.sgt.security;

import java.util.List;

/**
 * Códigos de permiso usados por la lógica de autorización a nivel de recurso
 * Los @PreAuthorize de los Controllers siguen usando el
 * literal; aquí solo están los que el código de negocio consulta.
 */
public final class Permisos {

    /** Alcance global: ver todas las Solicitudes, no solo las propias. */
    public static final String SOLICITUD_READ_ALL = "solicitud.read_all";
    /** Alcance global: ver todos los Requerimientos. */
    public static final String REQUERIMIENTO_READ_ALL = "requerimiento.read_all";
    /** Alcance global: ver todas las Órdenes de Trabajo. */
    public static final String ORDEN_READ_ALL = "orden.read_all";

    /**
     * Lecturas que todo rol tiene por defecto (decisión 2026-10-09): se suman
     * a los permisos del rol al emitir el JWT y no se asignan por rol_permiso.
     * No amplían el alcance: sin *.read_all cada usuario solo ve lo propio
     * (AutorizacionRecursoService). Nunca incluir usuario/rol/permiso.
     */
    public static final List<String> LECTURA_BASE = List.of(
            "solicitud.read", "requerimiento.read", "orden.read",
            "activo.read", "estado.read", "especialidad.read",
            "historial_solicitud.read", "historial_requerimiento.read", "historial_orden.read");

    private Permisos() {
    }
}
