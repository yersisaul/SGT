package cfbd.co.sgt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

@Entity
@Table(name = "aprobacion")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class Aprobacion {
    @Id
    @Column(name = "id_aprobacion")
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id_aprobacion;

    // Relación con la entidad Requerimiento (muchas aprobaciones pueden pertenecer a un requerimiento)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_requerimiento", nullable = false)
    private Requerimiento requerimiento;

    // Relacion con la entidad Usuario (muchas aprobaciones pueden ser realizadas por un usuario)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "aprobado")
    private Boolean aprobado;

    @Column (name = "comentario")
    private String comentario;

    @Column(name = "fecha_aprobacion")
    private Instant fecha_aprobacion;

    @Column (name = "url_adjunto")
    private String url_adjunto;
    
}
