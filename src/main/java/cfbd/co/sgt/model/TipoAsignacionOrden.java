package cfbd.co.sgt.model;

/** Eventos de asignación de una OT (PRD FR-018), base de los KPIs de cola y ruteo. */
public enum TipoAsignacionOrden {
    /** La OT entra a la cola de una especialidad (al generarse). */
    ENCOLADA,
    /** Un miembro la toma de la cola. */
    TOMADA,
    /** El responsable la asigna a un miembro. */
    ASIGNADA,
    /** El ejecutor confirma que le corresponde (paso 11: sí). */
    CONFIRMADA,
    /** El ejecutor declara que no le corresponde (paso 11: no). */
    DEVUELTA,
    /** Responsable o Administrador la mueve a otra especialidad (paso 12). */
    REASIGNADA_ESPECIALIDAD
}
