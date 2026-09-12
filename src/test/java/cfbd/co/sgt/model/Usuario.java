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
@Table(name = "usuario")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Usuario {
    @Id
    @Column(name = "id_usuario")
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id_usuario;

    // Relación con la entidad Rol (muchos usuarios pertenecen a un rol)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash")
    private String password_hash;

    @Column(name = "nombres")
    private String nombres;

    @Column(name = "apellidos")
    private String apellidos;

    @Column(name = "url_img")
    private String url_img;

    // Relación con la entidad Aprobacion (un usuario puede tener muchas aprobaciones)
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<Aprobacion> aprobaciones = new ArrayList<>();

    // Relación con la entidad Requerimiento (un usuario puede tener muchos requerimientos)
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<Requerimiento> requerimientos = new ArrayList<>();

    // Relacion con la entidad Derivacion (un usuario puede ser el origen de muchas derivaciones)
    @OneToMany(mappedBy = "usuario_origen", fetch = FetchType.LAZY)
    private List<Derivacion> derivaciones_origen = new ArrayList<>();

    // Relacion con la entidad Derivacion (un usuario puede ser el destino de muchas derivaciones)
    @OneToMany(mappedBy = "usuario_destino", fetch = FetchType.LAZY)
    private List<Derivacion> derivaciones_destino = new ArrayList<>();

    // Relación con la entidad HistorialOrden (un usuario puede tener muchos historiales de orden)
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<HistorialOrden> historiales_ordenes = new ArrayList<>();

    // Relación con la entidad HistorialRequerimiento (un usuario puede tener muchos historiales de requerimiento)
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<HistorialRequerimiento> historiales_requerimientos = new ArrayList<>();

    // Relación con la entidad HistorialSolicitud (un usuario puede tener muchos historiales de solicitud)
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<HistorialSolicitud> historiales_solicitudes = new ArrayList<>();

    // Relación con la entidad Orden (un usuario puede tener muchas órdenes)
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<Orden> ordenes = new ArrayList<>();

    // Relación con la entidad Solicitud (un usuario puede tener muchas solicitudes)
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<Solicitud> solicitudes = new ArrayList<>();
}
