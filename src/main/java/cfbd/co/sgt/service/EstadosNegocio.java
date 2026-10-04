package cfbd.co.sgt.service;

/** Nombres del catálogo de estados usados por el flujo (DataSeeder.seedEstados). */
public final class EstadosNegocio {

    public static final String PENDIENTE = "Pendiente";
    public static final String EN_REVISION = "En revisión";
    public static final String EN_PROGRESO = "En progreso";
    public static final String FINALIZADO = "Finalizado";
    public static final String APROBADO = "Aprobado";
    public static final String RECHAZADO = "Rechazado";
    /** OT con ejecutor que aún no confirma si le corresponde (PRD D13). */
    public static final String ASIGNADA = "Asignada";
    /** OT que el ejecutor declaró que no le corresponde; espera al responsable (PRD D15). */
    public static final String DEVUELTA = "Devuelta";

    private EstadosNegocio() {
    }
}
