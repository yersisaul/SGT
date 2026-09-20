package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.Derivacion;
import java.util.UUID;

@Repository 
public interface DerivacionRepository extends JpaRepository<Derivacion, UUID>  {


}
