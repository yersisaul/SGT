package cfbd.co.sgt.repository;

import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.HistorialOrden;
import java.util.UUID;

@Repository 
public interface HistorialOrdenRepository extends JpaRepository<HistorialOrden, UUID>  {


    @Query("select h from HistorialOrden h where h.orden.id_orden = :idPadre order by h.fecha")
    List<HistorialOrden> findByPadre(@Param("idPadre") UUID idPadre);

    @Query("select h from HistorialOrden h where h.orden.id_orden in :idsPadre order by h.fecha")
    List<HistorialOrden> findByPadres(@Param("idsPadre") List<UUID> idsPadre);

    @Query("select case when count(h) > 0 then true else false end from HistorialOrden h where h.orden.id_orden = :idPadre")
    boolean existsByPadre(@Param("idPadre") UUID idPadre);
}
