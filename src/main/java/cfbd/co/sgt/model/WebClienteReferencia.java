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

/**
 * Empresa o institución que confía en CFBD (logos del sitio web). No es un
 * usuario del SGT: el rol "Cliente" del sistema de tickets es otra cosa.
 */
@Entity
@Table(name = "web_cliente_referencia")
@Getter
@Setter
@NoArgsConstructor
public class WebClienteReferencia extends AuditoriaWeb {

    @Id
    @Column(name = "id_cliente_referencia")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id_cliente_referencia;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    // Texto libre: "Retail", "Gobierno local"…
    @Column(name = "sector", length = 100)
    private String sector;

    // Referencia lógica del FileStorageService (no una URL).
    @Column(name = "url_logo")
    private String url_logo;

    @Column(name = "url_sitio", length = 500)
    private String url_sitio;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "publicado", nullable = false)
    private boolean publicado;
}
