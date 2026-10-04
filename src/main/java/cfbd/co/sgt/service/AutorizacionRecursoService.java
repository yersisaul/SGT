package cfbd.co.sgt.service;

import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.model.Usuario;

/**
 * Autorización a nivel de recurso, basada en permisos de
 * alcance (solicitud.read_all, requerimiento.read_all, orden.read_all) y en
 * la relación del actor con el recurso, nunca en el nombre del rol (6.2).
 * El permiso de la operación (p. ej. solicitud.read) lo valida antes el
 * @PreAuthorize del Controller.
 */
public interface AutorizacionRecursoService {

    boolean veTodasLasSolicitudes();

    boolean veTodosLosRequerimientos();

    boolean veTodasLasOrdenes();

    boolean puedeVer(Usuario actor, Solicitud solicitud);

    boolean puedeVer(Usuario actor, Requerimiento requerimiento);

    boolean puedeVer(Usuario actor, Orden orden);

    /** El actor pertenece al equipo de la especialidad (PRD E2). */
    boolean esMiembro(Usuario actor, Especialidad especialidad);

    /** El actor es responsable de la especialidad (PRD D5). */
    boolean esResponsable(Usuario actor, Especialidad especialidad);

    /** Lanza 404 si el actor no puede ver la Solicitud (no revela su existencia). */
    void exigirVisible(Usuario actor, Solicitud solicitud);

    void exigirVisible(Usuario actor, Requerimiento requerimiento);

    void exigirVisible(Usuario actor, Orden orden);
}
