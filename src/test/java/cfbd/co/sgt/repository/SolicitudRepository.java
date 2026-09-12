package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.Solicitud;
import java.util.UUID;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, UUID> {    
    Optional<Solicitud> findByNumeroSolicitud(String numeroSolicitud);
}
