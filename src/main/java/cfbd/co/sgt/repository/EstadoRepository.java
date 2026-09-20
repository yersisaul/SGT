package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.Estado;
import java.util.UUID;

@Repository 
public interface EstadoRepository extends JpaRepository<Estado, UUID>  {
    // Buscar estado por nombre
    Optional<Estado> findByNombre(String nombre);

}
