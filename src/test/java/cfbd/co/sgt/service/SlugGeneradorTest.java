package cfbd.co.sgt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

class SlugGeneradorTest {

    @Test
    void quitaTildesEspaciosYSimbolos() {
        assertThat(SlugGenerador.generar("Azor Analytics 2.0")).isEqualTo("azor-analytics-2-0");
        assertThat(SlugGenerador.generar("  Configuración y mantenimiento de VMS ")).isEqualTo("configuracion-y-mantenimiento-de-vms");
        assertThat(SlugGenerador.generar("Ñandú & Cía. — Perú")).isEqualTo("nandu-cia-peru");
        assertThat(SlugGenerador.generar("<script>alert(1)</script>")).isEqualTo("script-alert-1-script");
    }

    @Test
    void recortaALaLongitudMaximaSinGuionFinal() {
        String largo = "palabra ".repeat(40);
        String slug = SlugGenerador.generar(largo);
        assertThat(slug.length()).isLessThanOrEqualTo(SlugGenerador.LONGITUD_MAXIMA);
        assertThat(slug).doesNotEndWith("-");
    }

    @Test
    void agregaSufijoNumericoSiElSlugYaExiste() {
        Set<String> existentes = Set.of("azor-panel", "azor-panel-2");
        assertThat(SlugGenerador.generarUnico("Azor Panel", existentes::contains)).isEqualTo("azor-panel-3");
        assertThat(SlugGenerador.generarUnico("App Reporter", existentes::contains)).isEqualTo("app-reporter");
    }

    @Test
    void elSufijoNoExcedeLaLongitudMaxima() {
        String base = "a".repeat(SlugGenerador.LONGITUD_MAXIMA);
        String unico = SlugGenerador.generarUnico(base, base::equals);
        assertThat(unico).hasSize(SlugGenerador.LONGITUD_MAXIMA).endsWith("-2");
    }

    @Test
    void rechazaTextoSinLetrasNiNumeros() {
        assertThatThrownBy(() -> SlugGenerador.generarUnico("¿¡ !?", s -> false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
