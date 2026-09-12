package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.Permiso;
import java.util.UUID;

@Repository 
public interface PermisoRepository extends JpaRepository<Permiso, UUID> {

    Optional<Permiso> findByCodigo(String codigo);

}
