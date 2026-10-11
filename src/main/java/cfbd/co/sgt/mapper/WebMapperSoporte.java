package cfbd.co.sgt.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import cfbd.co.sgt.model.Usuario;

/**
 * Utilidades comunes de los mappers web_*. Las URLs de imagen llevan
 * ?v=&lt;fecha_actualizacion&gt; para que el navegador (que las cachea)
 * descargue la nueva al reemplazarla.
 */
final class WebMapperSoporte {

    private WebMapperSoporte() {
    }

    /** Para el panel: GET /api/web/{recurso}/{id}/imagen (con permiso de lectura). */
    static String imagenAdmin(String recurso, UUID id, Instant version) {
        return archivoAdmin(recurso, id, "imagen", version);
    }

    /** Para el panel: GET /api/web/{recurso}/{id}/{archivo} (p. ej. "logo"). */
    static String archivoAdmin(String recurso, UUID id, String archivo, Instant version) {
        return "/api/web/" + recurso + "/" + id + "/" + archivo + sufijoVersion(version);
    }

    /** Para el sitio: GET /api/public/web/archivos/{recurso}/{id} (solo si está publicado). */
    static String imagenPublica(String recurso, UUID id, Instant version) {
        return "/api/public/web/archivos/" + recurso + "/" + id + sufijoVersion(version);
    }

    private static String sufijoVersion(Instant version) {
        return version == null ? "" : "?v=" + version.toEpochMilli();
    }

    static <T> List<T> listaOVacia(List<T> lista) {
        return lista == null ? List.of() : lista;
    }

    static String nombreCompleto(Usuario usuario) {
        return usuario == null ? null : usuario.getNombres() + " " + usuario.getApellidos();
    }
}
