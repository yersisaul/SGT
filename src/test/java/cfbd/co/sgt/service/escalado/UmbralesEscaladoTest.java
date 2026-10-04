package cfbd.co.sgt.service.escalado;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class UmbralesEscaladoTest {

    private final UmbralesEscalado umbrales = new UmbralesEscalado(30, 120, 240);

    @Test
    void altaEscalaAResponsablesALos30MinutosYAlAdministradorALos60() {
        assertThat(umbrales.nivel(Duration.ofMinutes(29), "Alta")).isEqualTo(UmbralesEscalado.SIN_ESCALAR);
        assertThat(umbrales.nivel(Duration.ofMinutes(30), "Alta")).isEqualTo(UmbralesEscalado.NIVEL_RESPONSABLE);
        assertThat(umbrales.nivel(Duration.ofMinutes(60), "alta")).isEqualTo(UmbralesEscalado.NIVEL_ADMINISTRADOR);
    }

    @Test
    void mediaYBajaUsanSusPropiosUmbrales() {
        assertThat(umbrales.nivel(Duration.ofMinutes(119), "Media")).isEqualTo(UmbralesEscalado.SIN_ESCALAR);
        assertThat(umbrales.nivel(Duration.ofMinutes(120), "Media")).isEqualTo(UmbralesEscalado.NIVEL_RESPONSABLE);
        assertThat(umbrales.nivel(Duration.ofMinutes(479), "Baja")).isEqualTo(UmbralesEscalado.NIVEL_RESPONSABLE);
        assertThat(umbrales.nivel(Duration.ofMinutes(480), "Baja")).isEqualTo(UmbralesEscalado.NIVEL_ADMINISTRADOR);
    }

    @Test
    void prioridadDesconocidaONulaSeTrataComoMedia() {
        assertThat(umbrales.umbral(null)).isEqualTo(Duration.ofMinutes(120));
        assertThat(umbrales.umbral("Urgente")).isEqualTo(Duration.ofMinutes(120));
    }
}
