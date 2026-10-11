package cfbd.co.sgt.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Partner tecnológico o marca compatible que se muestra en el sitio web. */
@Entity
@Table(name = "web_partner")
@Getter
@Setter
@NoArgsConstructor
public class WebPartner extends AuditoriaWeb {

    @Id
    @Column(name = "id_partner")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id_partner;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    // Texto libre: "VMS", "HCI", "Ciberseguridad"…
    @Column(name = "categoria", length = 100)
    private String categoria;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    // Referencia lógica del FileStorageService (no una URL).
    @Column(name = "url_logo")
    private String url_logo;

    @Column(name = "url_sitio", length = 500)
    private String url_sitio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoPartner tipo;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "publicado", nullable = false)
    private boolean publicado;
}
