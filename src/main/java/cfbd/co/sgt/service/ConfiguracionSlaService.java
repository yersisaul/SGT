package cfbd.co.sgt.service;

import java.util.List;

import cfbd.co.sgt.dto.request.ConfiguracionSlaRequest;
import cfbd.co.sgt.dto.response.ConfiguracionSlaResponse;
import cfbd.co.sgt.model.ClaveSla;

/** SLA de despacho y atención guardados en BD y editables desde el panel (aprobado 2026-10-19). */
public interface ConfiguracionSlaService {

    /** Crea las claves que falten con el valor del .env (lo invoca el DataSeeder). */
    void inicializar();

    List<ConfiguracionSlaResponse> listar();

    List<ConfiguracionSlaResponse> actualizar(ConfiguracionSlaRequest request);

    /** Horas vigentes de un SLA (en caché; se recarga tras cada cambio confirmado). */
    long horas(ClaveSla clave);
}
