package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.Especialidad;
import java.util.UUID;

@Repository 
public interface EspecialidadRepository extends JpaRepository<Especialidad, UUID>  {
    // Buscar especialidad por nombre
    Optional<Especialidad> findByNombre(String nombre);

}
