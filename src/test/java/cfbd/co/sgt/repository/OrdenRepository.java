package cfbd.co.sgt.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.Orden;
import java.util.UUID;
import java.util.List;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, UUID> {
    // Buscar orden por número de orden
    Optional<Orden> findByNumeroOrden(String numeroOrden);

}
