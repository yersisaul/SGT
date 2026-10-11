package cfbd.co.sgt.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Producto del sitio web público (Azor Panel, Azor Analytics, apps…). */
@Entity
@Table(name = "web_producto")
@Getter
@Setter
@NoArgsConstructor
public class WebProducto extends AuditoriaWeb {

    @Id
    @Column(name = "id_producto")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id_producto;

    // Identificador en la URL del sitio (/productos/azor-analytics).
    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "subtitulo", length = 200)
    private String subtitulo;

    // Texto de la tarjeta del listado.
    @Column(name = "descripcion_corta", length = 300)
    private String descripcion_corta;

    // Texto de la página de detalle.
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // Referencia lógica del FileStorageService (no una URL).
    @Column(name = "url_img")
    private String url_img;

    // Enlace externo (YouTube); los videos no se suben al servidor.
    @Column(name = "url_video", length = 500)
    private String url_video;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "caracteristicas", columnDefinition = "jsonb")
    private List<String> caracteristicas = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modulos", columnDefinition = "jsonb")
    private List<ModuloProducto> modulos = new ArrayList<>();

    @Column(name = "destacado", nullable = false)
    private boolean destacado;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "publicado", nullable = false)
    private boolean publicado;
}
