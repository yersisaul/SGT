package cfbd.co.sgt.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Categoría de las novedades del sitio web (Noticias, Eventos…). */
@Entity
@Table(name = "web_categoria_novedad")
@Getter
@Setter
@NoArgsConstructor
public class WebCategoriaNovedad extends AuditoriaWeb {

    @Id
    @Column(name = "id_categoria_novedad")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id_categoria_novedad;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // Filtro del sitio: /novedades?categoria=eventos
    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "orden", nullable = false)
    private int orden;
}
