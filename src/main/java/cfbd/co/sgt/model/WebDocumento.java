package cfbd.co.sgt.model;

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

/** PDF descargable del sitio web: brochure, ficha técnica o presentación. */
@Entity
@Table(name = "web_documento")
@Getter
@Setter
@NoArgsConstructor
public class WebDocumento extends AuditoriaWeb {

    @Id
    @Column(name = "id_documento")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id_documento;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoDocumentoWeb tipo;

    // Referencia lógica del FileStorageService; null hasta que se sube el PDF.
    @Column(name = "ruta_archivo")
    private String ruta_archivo;

    // Nombre con el que se descarga (el físico es un UUID).
    @Column(name = "nombre_original", length = 255)
    private String nombre_original;

    // Lo calcula el servidor al subir el archivo.
    @Column(name = "tamano_bytes")
    private Long tamano_bytes;

    // Opcional. Un producto con documentos no se puede borrar (409).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto")
    private WebProducto producto;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "publicado", nullable = false)
    private boolean publicado;
}
