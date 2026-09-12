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
@Table(name = "historial_requerimiento")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class HistorialRequerimiento {
    @Id
    @Column(name = "id_historial_requerimiento")
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id_historial_requerimiento;

    // Relación con la entidad Requerimiento (un historial de requerimiento pertenece a un requerimiento)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_requerimiento", nullable = false)
    private Requerimiento requerimiento;

    // Relación con la entidad Usuario (un historial de requerimiento pertenece a un usuario)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // Relación con la entidad Estado (un historial de requerimiento tiene un estado anterior)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_anterior", nullable = false)
    private Estado estado_anterior;

    // Relación con la entidad Estado (un historial de requerimiento tiene un estado nuevo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_nuevo", nullable = false)
    private Estado estado_nuevo;
    
    @Column(name = "fecha")
    private Instant fecha;
    
    @Column (name = "comentario")
    private String comentario;
}
