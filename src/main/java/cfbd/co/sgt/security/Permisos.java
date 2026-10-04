package cfbd.co.sgt.security;

/**
 * Códigos de permiso usados por la lógica de autorización a nivel de recurso
 * (CLAUDE.md 6.2/6.5). Los @PreAuthorize de los Controllers siguen usando el
 * literal; aquí solo están los que el código de negocio consulta.
 */
public final class Permisos {

    /** Alcance global: ver todas las Solicitudes, no solo las propias. */
    public static final String SOLICITUD_READ_ALL = "solicitud.read_all";
    /** Alcance global: ver todos los Requerimientos. */
    public static final String REQUERIMIENTO_READ_ALL = "requerimiento.read_all";
    /** Alcance global: ver todas las Órdenes de Trabajo. */
    public static final String ORDEN_READ_ALL = "orden.read_all";

    private Permisos() {
    }
}
