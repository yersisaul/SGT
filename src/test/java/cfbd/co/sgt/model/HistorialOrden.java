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
@Table(name = "historial_orden")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class HistorialOrden {
    @Id
    @Column(name = "id_historial_orden")
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id_historial_orden;

    // Relación con la entidad Orden (un historial de orden pertenece a una orden)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden", nullable = false)
    private Orden orden;

    // Relación con la entidad Usuario (un historial de orden pertenece a un usuario)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // Relación con la entidad Estado (un historial de orden pertenece a un estado)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_anterior", nullable = false)
    private Estado estado_anterior;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_nuevo", nullable = false)
    private Estado estado_nuevo;
    
    @Column(name = "fecha")
    private Instant fecha;

    @Column (name = "comentario")
    private String comentario;

}
