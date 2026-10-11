package cfbd.co.sgt.model;

/** Ciclo de vida de una novedad del sitio web. */
public enum EstadoNovedad {
    /** En redacción: solo se ve en el panel. */
    BORRADOR,
    /** Visible en el sitio desde su fecha_publicacion (si es futura, queda programada). */
    PUBLICADO,
    /** Retirada del sitio sin borrarla. */
    ARCHIVADO
}
