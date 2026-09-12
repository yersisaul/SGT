package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.HistorialRequerimiento;
import java.util.UUID;

@Repository
public interface HistorialRequerimientoRepository extends JpaRepository<HistorialRequerimiento, UUID> {

}
