package cfbd.co.sgt.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Identificador legible para las URLs del sitio web público
 * ("Azor Analytics 2.0" → "azor-analytics-2-0"): sin tildes, en minúsculas,
 * solo letras, números y guiones.
 */
public final class SlugGenerador {

    public static final int LONGITUD_MAXIMA = 120;

    private SlugGenerador() {
    }

    public static String generar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String slug = sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slug.length() > LONGITUD_MAXIMA) {
            slug = slug.substring(0, LONGITUD_MAXIMA).replaceAll("-+$", "");
        }
        return slug;
    }

    /**
     * Slug de {@code texto} que no exista todavía: si "azor" está tomado
     * prueba "azor-2", "azor-3"… {@code existe} consulta el repository del
     * recurso (excluyendo el propio registro al editar).
     */
    public static String generarUnico(String texto, Predicate<String> existe) {
        String base = generar(texto);
        if (base.isEmpty()) {
            throw new IllegalArgumentException("No se puede generar un slug a partir de un texto sin letras ni números.");
        }
        String candidato = base;
        for (int sufijo = 2; existe.test(candidato); sufijo++) {
            String terminacion = "-" + sufijo;
            String recortada = base.length() + terminacion.length() > LONGITUD_MAXIMA
                    ? base.substring(0, LONGITUD_MAXIMA - terminacion.length()).replaceAll("-+$", "")
                    : base;
            candidato = recortada + terminacion;
        }
        return candidato;
    }
}
