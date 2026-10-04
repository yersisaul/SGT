package cfbd.co.sgt.service.escalado;

import java.util.List;

import cfbd.co.sgt.dto.response.OrdenEscaladaResponse;

/** Detección de OT que llevan demasiado tiempo en cola sin tomar (decisión 2026-10-04). */
public interface EscaladoColaService {

    /** Todas las OT escaladas en este momento (uso interno del job). */
    List<OrdenEscaladaResponse> detectar();

    /** Las visibles para el actor: todas con orden.read_all; si no, las de sus especialidades como responsable. */
    List<OrdenEscaladaResponse> listarVisibles();
}
