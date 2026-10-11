package cfbd.co.sgt.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import cfbd.co.sgt.exception.DuplicateResourceException;

/**
 * Normalización común de los textos que llegan del panel del sitio web:
 * recorta espacios, convierte vacíos en null y resuelve el slug.
 */
public final class WebTextos {

    private WebTextos() {
    }

    /** Texto recortado; null si viene vacío o solo con espacios. */
    public static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    /** Lista (mutable) de textos recortados, sin vacíos; nunca null. */
    public static List<String> limpiarLista(List<String> textos) {
        List<String> resultado = new ArrayList<>();
        if (textos != null) {
            textos.stream().map(WebTextos::limpiar).filter(t -> t != null).forEach(resultado::add);
        }
        return resultado;
    }

    /**
     * Slug para un alta: el enviado (normalizado; 409 si ya existe) o, si no
     * llega, uno único generado desde el nombre.
     */
    public static String slugParaAlta(String slugEnviado, String nombre, Predicate<String> existe, String recurso) {
        if (limpiar(slugEnviado) == null) {
            return SlugGenerador.generarUnico(nombre, existe);
        }
        String slug = normalizarSlug(slugEnviado);
        if (existe.test(slug)) {
            throw new DuplicateResourceException("Ya existe " + recurso + " con el slug '" + slug + "'.");
        }
        return slug;
    }

    /**
     * Slug para una edición: si no llega se conserva el actual (cambiarlo
     * rompe los enlaces ya compartidos); si llega, normalizado y único.
     */
    public static String slugParaEdicion(String slugEnviado, String slugActual, Predicate<String> existeEnOtro,
            String recurso) {
        if (limpiar(slugEnviado) == null) {
            return slugActual;
        }
        String slug = normalizarSlug(slugEnviado);
        if (!slug.equals(slugActual) && existeEnOtro.test(slug)) {
            throw new DuplicateResourceException("Ya existe " + recurso + " con el slug '" + slug + "'.");
        }
        return slug;
    }

    private static String normalizarSlug(String slugEnviado) {
        String slug = SlugGenerador.generar(slugEnviado);
        if (slug.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El slug debe tener letras o números.");
        }
        return slug;
    }
}
