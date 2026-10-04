package cfbd.co.sgt.repository;

import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.HistorialRequerimiento;
import java.util.UUID;

@Repository
public interface HistorialRequerimientoRepository extends JpaRepository<HistorialRequerimiento, UUID> {


    @Query("select h from HistorialRequerimiento h where h.requerimiento.id_requerimiento = :idPadre order by h.fecha")
    List<HistorialRequerimiento> findByPadre(@Param("idPadre") UUID idPadre);

    @Query("select h from HistorialRequerimiento h where h.requerimiento.id_requerimiento in :idsPadre order by h.fecha")
    List<HistorialRequerimiento> findByPadres(@Param("idsPadre") List<UUID> idsPadre);

    @Query("select case when count(h) > 0 then true else false end from HistorialRequerimiento h where h.requerimiento.id_requerimiento = :idPadre")
    boolean existsByPadre(@Param("idPadre") UUID idPadre);
}
