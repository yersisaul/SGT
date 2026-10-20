package cfbd.co.sgt.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cfbd.co.sgt.model.ClaveSla;
import cfbd.co.sgt.model.ConfiguracionSla;

public interface ConfiguracionSlaRepository extends JpaRepository<ConfiguracionSla, UUID> {

    Optional<ConfiguracionSla> findByClave(ClaveSla clave);
}
