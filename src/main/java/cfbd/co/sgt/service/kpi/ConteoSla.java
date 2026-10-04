package cfbd.co.sgt.service.kpi;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Acumulador del KPI de SLA de despacho para un grupo (una prioridad o el total). */
class ConteoSla {

    long registradas;
    long despachadas;
    long dentro;
    long fuera;
    long sinDespachar;

    void registrar(Instant despacho, Instant limite, Instant ahora) {
        registradas++;
        if (despacho == null) {
            sinDespachar++;
            if (ahora.isAfter(limite)) {
                fuera++;
            }
            return;
        }
        despachadas++;
        if (despacho.isAfter(limite)) {
            fuera++;
        } else {
            dentro++;
        }
    }

    Double cumplimiento() {
        return Estadistica.porcentaje(dentro, dentro + fuera);
    }

    Map<String, Object> fila(String prioridad) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("prioridad", prioridad);
        fila.put("registradas", registradas);
        fila.put("despachadas", despachadas);
        fila.put("dentro_sla", dentro);
        fila.put("fuera_sla", fuera);
        fila.put("cumplimiento", cumplimiento());
        return fila;
    }
}
