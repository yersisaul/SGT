package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.RolPermiso;
import java.util.UUID;

@Repository
public interface RolPermisoRepository extends JpaRepository<RolPermiso, UUID> {

}
