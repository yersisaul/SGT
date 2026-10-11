package cfbd.co.sgt.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Novedad (noticia) del sitio web. Se ve en el sitio solo si está PUBLICADO y
 * su fecha_publicacion ya llegó; una fecha futura la deja programada.
 */
@Entity
@Table(name = "web_novedad")
@Getter
@Setter
@NoArgsConstructor
public class WebNovedad extends AuditoriaWeb {

    @Id
    @Column(name = "id_novedad")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id_novedad;

    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "resumen", length = 500)
    private String resumen;

    // Texto plano (párrafos separados por línea en blanco): el sitio lo pinta
    // con textContent, sin HTML, para evitar XSS.
    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    // Referencia lógica del FileStorageService (no una URL).
    @Column(name = "url_portada")
    private String url_portada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoNovedad estado;

    @Column(name = "fecha_publicacion")
    private Instant fecha_publicacion;

    // Opcional. Una categoría con novedades no se puede borrar (409).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria_novedad")
    private WebCategoriaNovedad categoria;

    // Quien la creó; no cambia al editarla (eso queda en usuario_actualiza).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_autor")
    private Usuario autor;

    @Column(name = "destacado", nullable = false)
    private boolean destacado;

    /** Visible en el sitio en el instante indicado. */
    public boolean esVisibleEn(Instant instante) {
        return estado == EstadoNovedad.PUBLICADO && fecha_publicacion != null && !fecha_publicacion.isAfter(instante);
    }
}
