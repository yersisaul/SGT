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
@Table(name = "permiso")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class Permiso {
    @Id
    @Column(name = "id_permiso")
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id_permiso;

    @Column(name = "codigo")
    private String codigo;

    @Column(name = "descripcion")
    private String descripcion;

    // Relación con la entidad RolPermiso (un permiso puede estar asociado a muchos roles)
    @OneToMany (mappedBy = "id_permiso", fetch = FetchType.LAZY)
    private List<RolPermiso> rolPermisos = new ArrayList<>();

}
