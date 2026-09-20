package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.RolPermiso;
import cfbd.co.sgt.model.Rol;
import cfbd.co.sgt.model.Permiso;
import java.util.List;
import java.util.UUID;

@Repository
public interface RolPermisoRepository extends JpaRepository<RolPermiso, UUID> {

    // Derivado como existsBy... falla en esta versión de Spring Data JPA
    // porque el id de RolPermiso no se llama "id" (ver id_rol_permiso);
    // se usa JPQL explícito para evitar el bug de proyección del exists derivado.
    @Query("select case when count(rp) > 0 then true else false end "
            + "from RolPermiso rp where rp.rol = :rol and rp.permiso = :permiso")
    boolean existsByRolAndPermiso(@Param("rol") Rol rol, @Param("permiso") Permiso permiso);

    List<RolPermiso> findByRol(Rol rol);
}
