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
@Table(name = "requerimiento")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class Requerimiento {
    @Id
    @Column(name = "id_requerimiento")
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id_requerimiento;

    // Relación con la entidad Usuario (muchos requerimientos pertenecen a un usuario)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // Relación con la entidad Estado (muchos requerimientos pertenecen a un estado)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    // Relación con la entidad Especialidad (muchos requerimientos pertenecen a una especialidad)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidad especialidad;

    @Column (name = "numero_requerimiento")
    private String numeroRequerimiento;

    @Column (name = "fecha_registro")
    private Instant fecha_registro;

    @Column (name = "descripcion")
    private String descripcion;

    @Column (name = "url_adjunto")
    private String url_adjunto;

    // Relación con la entidad Aprobacion (un requerimiento puede tener muchas aprobaciones)
    @OneToMany(mappedBy = "id_requerimiento", fetch = FetchType.LAZY)
    private List<Aprobacion> aprobaciones = new ArrayList<>();

    // Relación con la entidad HistorialRequerimiento (un requerimiento puede tener muchos historiales de requerimiento)
    @OneToMany(mappedBy = "requerimiento", fetch = FetchType.LAZY)
    private List<HistorialRequerimiento> historiales = new ArrayList<>();

    // Relación con la entidad Orden (un requerimiento puede tener muchas órdenes)
    @OneToMany(mappedBy = "requerimiento", fetch = FetchType.LAZY)
    private List<Orden> ordenes = new ArrayList<>();

}
