package cfbd.co.sgt.service;

/**
 * Descarga en CSV de Solicitudes, Requerimientos y Órdenes (observación
 * 2026-10-19). Exporta exactamente lo que el actor puede ver en el listado
 * (misma regla de visibilidad), con los nombres ya resueltos.
 */
public interface ExportacionService {

    String exportarSolicitudes();

    String exportarRequerimientos();

    String exportarOrdenes();
}
