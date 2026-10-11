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

/** Servicio del sitio web público (servidores, VMS, modelos de visión…). */
@Entity
@Table(name = "web_servicio")
@Getter
@Setter
@NoArgsConstructor
public class WebServicio extends AuditoriaWeb {

    @Id
    @Column(name = "id_servicio")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id_servicio;

    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion_corta", length = 300)
    private String descripcion_corta;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // Nombre de un ícono de Font Awesome 6 (p. ej. "server"), sin prefijo "fa-".
    @Column(name = "icono", length = 60)
    private String icono;

    // Referencia lógica del FileStorageService (no una URL).
    @Column(name = "url_img")
    private String url_img;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "caracteristicas", columnDefinition = "jsonb")
    private List<String> caracteristicas = new ArrayList<>();

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "publicado", nullable = false)
    private boolean publicado;
}
