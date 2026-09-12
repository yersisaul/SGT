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
@Table(name = "activo")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Activo {
    @Id
    @GeneratedValue (strategy = GenerationType.UUID)
    @Column(name = "id_activo")
    private UUID id_activo;
    
    // Relación con la entidad Especialidad (muchos activos pueden pertenecer a una especialidad)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidad especialidad;

    @Column(name = "codigo")
    private String codigo;

    @Column (name = "nombre")
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column (name = "ubicacion")
    private String ubicacion;

    @Column (name = "url_img")
    private String url_img;

    // Relación con la entidad Solicitud (un activo puede tener muchas solicitudes)
    @OneToMany(mappedBy = "activo", fetch = FetchType.LAZY)
    private List<Solicitud> solicitudes = new ArrayList<>();    

}
