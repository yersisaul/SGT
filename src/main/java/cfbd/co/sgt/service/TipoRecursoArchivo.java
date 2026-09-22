package cfbd.co.sgt.service;

import java.util.Set;

/**
 * Recursos del sistema que aceptan un archivo (imagen o adjunto). Cada uno
 * define su carpeta física bajo el directorio raíz de almacenamiento
 * (app.storage.root) y qué tipos de archivo acepta.
 */
public enum TipoRecursoArchivo {
    USUARIOS("usuarios", Categoria.IMAGEN),
    ACTIVOS("activos", Categoria.IMAGEN),
    SOLICITUDES("solicitudes", Categoria.ADJUNTO),
    REQUERIMIENTOS("requerimientos", Categoria.ADJUNTO),
    APROBACIONES("aprobaciones", Categoria.ADJUNTO),
    ORDENES("ordenes", Categoria.ADJUNTO);

    public enum Categoria {
        IMAGEN, ADJUNTO
    }

    private static final Set<String> EXTENSIONES_IMAGEN = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> MIMES_IMAGEN = Set.of("image/jpeg", "image/png", "image/webp");

    private static final Set<String> EXTENSIONES_ADJUNTO = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final Set<String> MIMES_ADJUNTO = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");

    private final String carpeta;
    private final Categoria categoria;

    TipoRecursoArchivo(String carpeta, Categoria categoria) {
        this.carpeta = carpeta;
        this.categoria = categoria;
    }

    public String getCarpeta() {
        return carpeta;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Set<String> extensionesPermitidas() {
        return categoria == Categoria.IMAGEN ? EXTENSIONES_IMAGEN : EXTENSIONES_ADJUNTO;
    }

    public Set<String> mimesPermitidos() {
        return categoria == Categoria.IMAGEN ? MIMES_IMAGEN : MIMES_ADJUNTO;
    }
}
