package cfbd.co.sgt.service;

/**
 * Números correlativos legibles (ST-n, RQ-n, OT-n) respaldados por secuencias
 * de PostgreSQL: únicos con concurrencia y nunca reutilizados tras un
 * borrado (reemplaza el antiguo count() + 1).
 */
public interface NumeracionService {

    String siguienteNumeroSolicitud();

    String siguienteNumeroRequerimiento();

    String siguienteNumeroOrden();
}
