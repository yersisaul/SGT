package cfbd.co.sgt.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import cfbd.co.sgt.model.Requerimiento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequerimientoRepository extends  JpaRepository<Requerimiento, UUID>  {
    Optional<Requerimiento> findByNumeroRequerimiento(String numeroRequerimiento);

    @Query("select r.estado.nombre, count(r) from Requerimiento r group by r.estado.nombre")
    List<Object[]> countByEstado();
}
