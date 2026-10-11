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
    ORDENES("ordenes", Categoria.ADJUNTO),

    // Sitio web público (CMS). Sin SVG: puede llevar scripts (XSS).
    WEB_PRODUCTOS("web/productos", Categoria.IMAGEN),
    WEB_SERVICIOS("web/servicios", Categoria.IMAGEN),
    WEB_PARTNERS("web/partners", Categoria.IMAGEN),
    WEB_CLIENTES("web/clientes", Categoria.IMAGEN),
    WEB_CASOS_EXITO("web/casos-exito", Categoria.IMAGEN),
    WEB_NOVEDADES("web/novedades", Categoria.IMAGEN),
    WEB_DOCUMENTOS("web/documentos", Categoria.DOCUMENTO_WEB);

    /**
     * IMAGEN y ADJUNTO usan app.storage.image/document-max-size-mb;
     * DOCUMENTO_WEB (solo PDF: brochures, fichas técnicas) usa
     * app.storage.web-document-max-size-mb, porque esos PDFs son más pesados.
     */
    public enum Categoria {
        IMAGEN, ADJUNTO, DOCUMENTO_WEB
    }

    private static final Set<String> EXTENSIONES_IMAGEN = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> MIMES_IMAGEN = Set.of("image/jpeg", "image/png", "image/webp");

    private static final Set<String> EXTENSIONES_ADJUNTO = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final Set<String> MIMES_ADJUNTO = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");

    private static final Set<String> EXTENSIONES_DOCUMENTO_WEB = Set.of("pdf");
    private static final Set<String> MIMES_DOCUMENTO_WEB = Set.of("application/pdf");

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
        return switch (categoria) {
            case IMAGEN -> EXTENSIONES_IMAGEN;
            case ADJUNTO -> EXTENSIONES_ADJUNTO;
            case DOCUMENTO_WEB -> EXTENSIONES_DOCUMENTO_WEB;
        };
    }

    public Set<String> mimesPermitidos() {
        return switch (categoria) {
            case IMAGEN -> MIMES_IMAGEN;
            case ADJUNTO -> MIMES_ADJUNTO;
            case DOCUMENTO_WEB -> MIMES_DOCUMENTO_WEB;
        };
    }
}
