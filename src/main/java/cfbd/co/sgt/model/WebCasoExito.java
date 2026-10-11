package cfbd.co.sgt.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Caso de éxito del sitio web (p. ej. Alerta Surco). */
@Entity
@Table(name = "web_caso_exito")
@Getter
@Setter
@NoArgsConstructor
public class WebCasoExito extends AuditoriaWeb {

    @Id
    @Column(name = "id_caso_exito")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id_caso_exito;

    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "resumen", length = 500)
    private String resumen;

    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    // Referencia lógica del FileStorageService (no una URL).
    @Column(name = "url_img")
    private String url_img;

    // Nota de prensa, video del cliente, etc.
    @Column(name = "url_externa", length = 500)
    private String url_externa;

    @Column(name = "fecha_caso")
    private LocalDate fecha_caso;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metricas", columnDefinition = "jsonb")
    private List<MetricaCaso> metricas = new ArrayList<>();

    // Opcional: cliente del caso. Un cliente con casos no se puede borrar (409).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente_referencia")
    private WebClienteReferencia cliente;

    // Opcional: producto protagonista. Un producto con casos no se puede borrar (409).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto")
    private WebProducto producto;

    @Column(name = "destacado", nullable = false)
    private boolean destacado;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "publicado", nullable = false)
    private boolean publicado;
}
