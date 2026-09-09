package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import cfbd.co.sgt.model.Aprobacion;

@Repository 
public interface AprobacionRepository extends JpaRepository<Aprobacion, String> {

}
