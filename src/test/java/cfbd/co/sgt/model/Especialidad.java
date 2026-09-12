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

@Entity
@Table(name = "especialidad")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class Especialidad {
    @Id
    @Column(name = "id_especialidad")
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id_especialidad;

    @Column (name = "nombre")
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    // Relación con la entidad Activo (una especialidad puede tener muchos activos)
    @OneToMany(mappedBy = "id_especialidad", fetch = FetchType.LAZY)
    private List<Activo> activos = new ArrayList<>();

    // Relación con la entidad Requerimiento (una especialidad puede tener muchos requerimientos)
    @OneToMany(mappedBy = "id_especialidad", fetch = FetchType.LAZY)
    private List<Requerimiento> requerimientos = new ArrayList<>();

    // Relación con la entidad Orden (una especialidad puede tener muchas órdenes)
    @OneToMany(mappedBy = "especialidad", fetch = FetchType.LAZY)
    private List<Orden> ordenes = new ArrayList<>();

    // Relación con la entidad Solicitud (una especialidad puede tener muchas solicitudes)
    @OneToMany(mappedBy = "especialidad", fetch = FetchType.LAZY)
    private List<Solicitud> solicitudes = new ArrayList<>();

}
