package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.Activo;

@Repository 
public interface ActivoRepository extends  JpaRepository<Activo, String>  {

    Optional<Activo> findByNombre(String nombre);

    Optional<Activo> findByCodigo(String codigo);

}
