package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
import cfbd.co.sgt.model.Activo;

@Repository 
public interface ActivoRepository extends  JpaRepository<Activo, UUID>  {

    Optional<Activo> findByNombre(String nombre);
    Optional<Activo> findByCodigo(String codigo);

}
