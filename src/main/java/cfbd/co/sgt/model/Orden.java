package cfbd.co.sgt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

@Entity
@Table(name = "orden")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class Orden {
    @Id
    @Column(name = "id_orden")
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id_orden;

    // Relación con la entidad Usuario (una orden pertenece a un usuario)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // Relación con la entidad Estado (una orden tiene un estado)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    // Relación con la entidad Especialidad (una orden tiene una especialidad)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidad especialidad;

    // Relación con la entidad Solicitud (una orden pertenece a una solicitud)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = true)
    private Solicitud solicitud;

    // Relación con la entidad Requerimiento (una orden pertenece a un requerimiento)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_requerimiento", nullable = true)
    private Requerimiento requerimiento;

    @Column (name = "numero_orden")
    private String numeroOrden;

    @Column (name = "fecha_registro")
    private Instant fecha_registro;

    @Column (name = "fecha_cierre")
    private Instant fecha_cierre;

    @Column (name = "url_adjunto")
    private String url_adjunto;

    // Relación con la entidad HistorialOrden (una orden puede tener muchos historiales de orden)
    @OneToMany(mappedBy = "orden", fetch = FetchType.LAZY)
    private List<HistorialOrden> historiales = new ArrayList<>();
}
