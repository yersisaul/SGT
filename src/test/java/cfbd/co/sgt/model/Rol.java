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
@Table(name = "rol")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Rol {
    @Id
    @Column(name = "id_rol")
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id_rol;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    // Relación con la entidad Usuario (un rol puede tener muchos usuarios)
    @OneToMany (mappedBy = "id_rol", fetch = FetchType.LAZY)
    private List<Usuario> usuarios = new ArrayList<>();

    @OneToMany (mappedBy = "id_rol", fetch = FetchType.LAZY)
    private List<RolPermiso> rolPermisos = new ArrayList<>();

}
