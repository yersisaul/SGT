package cfbd.co.sgt.service.kpi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class EstadisticaYConteoTest {

    private static final Instant BASE = Instant.parse("2026-10-01T10:00:00Z");

    @Test
    void medianaDeTiemposConocidos() {
        // AC-031: tiempos de cola [1 h, 2 h, 3 h] → mediana 2 h.
        assertThat(Estadistica.percentil(List.of(3.0, 1.0, 2.0), 0.5)).isEqualTo(2.0);
    }

    @Test
    void p90InterpolaComoPercentileCont() {
        assertThat(Estadistica.percentil(List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0), 0.9)).isEqualTo(9.1);
    }

    @Test
    void sinDatosNoSeInventaUnValor() {
        assertThat(Estadistica.percentil(List.of(), 0.5)).isNull();
        assertThat(Estadistica.porcentaje(0, 0)).isNull();
    }

    @Test
    void tasaDeReasignacion() {
        // AC-033: 3 reasignadas de 20 → 15 %.
        assertThat(Estadistica.porcentaje(3, 20)).isEqualTo(15.0);
    }

    @Test
    void cumplimientoDeSlaConNueveDeDiezDentroDelPlazo() {
        // AC-030: 10 Solicitudes "Alta", 9 despachadas dentro de su SLA → 90 %.
        ConteoSla conteo = new ConteoSla();
        Instant limite = BASE.plus(Duration.ofHours(1));
        for (int i = 0; i < 9; i++) {
            conteo.registrar(BASE.plus(Duration.ofMinutes(30)), limite, BASE.plus(Duration.ofDays(1)));
        }
        conteo.registrar(BASE.plus(Duration.ofHours(2)), limite, BASE.plus(Duration.ofDays(1)));
        assertThat(conteo.cumplimiento()).isEqualTo(90.0);
    }

    @Test
    void pendienteVencidaCuentaFueraYPendienteEnPlazoNoCuenta() {
        ConteoSla conteo = new ConteoSla();
        Instant limite = BASE.plus(Duration.ofHours(1));
        conteo.registrar(null, limite, BASE.plus(Duration.ofHours(2)));
        conteo.registrar(null, limite, BASE.plus(Duration.ofMinutes(10)));
        assertThat(conteo.fuera).isEqualTo(1);
        assertThat(conteo.sinDespachar).isEqualTo(2);
        assertThat(conteo.cumplimiento()).isEqualTo(0.0);
    }
}
