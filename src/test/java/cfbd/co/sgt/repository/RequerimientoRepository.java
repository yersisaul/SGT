package cfbd.co.sgt.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import cfbd.co.sgt.model.Requerimiento;
import java.util.Optional;
import java.util.UUID;

@Repository 
public interface RequerimientoRepository extends  JpaRepository<Requerimiento, UUID>  {
    Optional<Requerimiento> findByNumeroRequerimiento(String numeroRequerimiento);

}
