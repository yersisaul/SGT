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
@Table(name = "solicitud")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class Solicitud {
    @Id
    @Column(name = "id_solicitud")
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id_solicitud;

    // Relación con la entidad Usuario (muchas solicitudes pertenecen a un usuario)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // Relación con la entidad Activo (muchas solicitudes pertenecen a un activo)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_activo", nullable = false)
    private Activo activo;

    // Relación con la entidad Estado (muchas solicitudes pertenecen a un estado)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    // Relación con la entidad Especialidad (muchas solicitudes pertenecen a una especialidad)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidad especialidad;

    @Column (name = "numero_solicitud")
    private String numeroSolicitud;

    @Column (name = "prioridad")
    private String prioridad;

    @Column (name = "fecha_registro")
    private Instant fecha_registro;

    @Column (name = "descripcion")
    private String descripcion;

    @Column (name = "url_adjunto")
    private String url_adjunto;

    // Relación con la entidad Derivacion (una solicitud puede tener muchas derivaciones)
    @OneToMany(mappedBy = "solicitud", fetch = FetchType.LAZY)
    private List<Derivacion> derivaciones = new ArrayList<>();

    // Relación con la entidad HistorialSolicitud (una solicitud puede tener muchos historiales de solicitud)
    @OneToMany(mappedBy = "solicitud", fetch = FetchType.LAZY)
    private List<HistorialSolicitud> historiales = new ArrayList<>();

    // Relación con la entidad Orden (una solicitud puede tener muchas órdenes)
    @OneToMany(mappedBy = "solicitud", fetch = FetchType.LAZY)
    private List<Orden> ordenes = new ArrayList<>();
}
