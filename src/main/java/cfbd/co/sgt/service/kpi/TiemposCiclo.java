package cfbd.co.sgt.service.kpi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Acumulador de duraciones (horas) de un grupo para el KPI de tiempos de ciclo. */
class TiemposCiclo {

    private static final double MEDIANA = 0.5;
    private static final double P90 = 0.9;

    long ordenes;
    final List<Double> cola = new ArrayList<>();
    final List<Double> ejecucion = new ArrayList<>();
    final List<Double> puntaAPunta = new ArrayList<>();

    Double mediana(List<Double> valores) {
        return Estadistica.percentil(valores, MEDIANA);
    }

    Double p90(List<Double> valores) {
        return Estadistica.percentil(valores, P90);
    }

    Map<String, Object> fila(String especialidad) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("especialidad", especialidad);
        fila.put("ordenes", ordenes);
        fila.put("cola_mediana", Estadistica.percentil(cola, MEDIANA));
        fila.put("cola_p90", Estadistica.percentil(cola, P90));
        fila.put("ejecucion_mediana", Estadistica.percentil(ejecucion, MEDIANA));
        fila.put("ejecucion_p90", Estadistica.percentil(ejecucion, P90));
        fila.put("total_mediana", Estadistica.percentil(puntaAPunta, MEDIANA));
        return fila;
    }
}
