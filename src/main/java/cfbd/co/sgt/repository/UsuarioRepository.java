package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.Usuario;
import java.util.List;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    // Buscar usuario por correo electrónico
    Optional<Usuario> findByEmail(String email);

    // Derivados como existsBy... fallan en esta versión de Spring Data JPA
    // porque el id de Usuario no se llama "id" (ver id_usuario); se usa
    // JPQL explícito para evitar el bug de proyección del exists derivado.
    @Query("select case when count(u) > 0 then true else false end from Usuario u where u.email = :email")
    boolean existsByEmail(@Param("email") String email);

    @Query("select case when count(u) > 0 then true else false end from Usuario u where u.nombres = :nombres")
    boolean existsByNombres(@Param("nombres") String nombres);

    @Query("select case when count(u) > 0 then true else false end from Usuario u where u.apellidos = :apellidos")
    boolean existsByApellidos(@Param("apellidos") String apellidos);

    // Usuarios de un rol dado (p. ej. "Operaciones" para el selector de
    // ejecutor). JPQL explícito por el mismo motivo que existsBy... arriba.
    @Query("select distinct u from Usuario u join u.rol.rolPermisos rp where rp.permiso.codigo = :codigoPermiso "
            + "order by u.nombres, u.apellidos")
    List<Usuario> findByPermiso(@Param("codigoPermiso") String codigoPermiso);
}
