package cfbd.co.sgt.service;

/** Tipos de aviso en tiempo real (PRD E5). El valor se envía como nombre del evento SSE. */
public enum TipoNotificacion {
    /** Una OT entró a la cola de la especialidad (nueva o reasignada). Destino: miembros. */
    ORDEN_ENCOLADA("orden.encolada"),
    /** Un responsable asignó la OT a un miembro. Destino: ese miembro. */
    ORDEN_ASIGNADA("orden.asignada"),
    /** El ejecutor devolvió la OT. Destino: responsables de la especialidad. */
    ORDEN_DEVUELTA("orden.devuelta"),
    /** La OT superó el umbral de espera en cola. Destino: responsables (nivel 1) o Administradores (nivel 2). */
    ORDEN_ESCALADA("orden.escalada");

    private final String evento;

    TipoNotificacion(String evento) {
        this.evento = evento;
    }

    public String evento() {
        return evento;
    }
}
