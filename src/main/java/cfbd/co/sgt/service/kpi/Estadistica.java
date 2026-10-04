package cfbd.co.sgt.service.kpi;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Utilidades numéricas de los KPIs. */
public final class Estadistica {

    private static final double SEGUNDOS_POR_HORA = 3600.0;
    private static final double DECIMALES = 10.0;
    private static final double CIEN = 100.0;

    private Estadistica() {
    }

    /** Percentil con interpolación lineal (equivalente a percentile_cont). null si no hay datos. */
    public static Double percentil(List<Double> valores, double p) {
        if (valores.isEmpty()) {
            return null;
        }
        List<Double> ordenados = valores.stream().sorted().toList();
        double posicion = p * (ordenados.size() - 1);
        int inferior = (int) Math.floor(posicion);
        int superior = (int) Math.ceil(posicion);
        double fraccion = posicion - inferior;
        return redondear(ordenados.get(inferior) + (ordenados.get(superior) - ordenados.get(inferior)) * fraccion);
    }

    public static double horasEntre(Instant inicio, Instant fin) {
        return Duration.between(inicio, fin).getSeconds() / SEGUNDOS_POR_HORA;
    }

    /** Porcentaje redondeado a 1 decimal; null si el denominador es 0. */
    public static Double porcentaje(long parte, long total) {
        return total == 0 ? null : redondear(parte * CIEN / total);
    }

    public static double redondear(double valor) {
        return Math.round(valor * DECIMALES) / DECIMALES;
    }
}
