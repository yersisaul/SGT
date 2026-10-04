package cfbd.co.sgt.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.HistorialSolicitud;
import java.util.UUID;

@Repository 
public interface HistorialSolicitudRepository extends JpaRepository<HistorialSolicitud, UUID>  {


    @Query("select h from HistorialSolicitud h where h.solicitud.id_solicitud = :idPadre order by h.fecha")
    List<HistorialSolicitud> findByPadre(@Param("idPadre") UUID idPadre);

    @Query("select h from HistorialSolicitud h where h.solicitud.id_solicitud in :idsPadre order by h.fecha")
    List<HistorialSolicitud> findByPadres(@Param("idsPadre") List<UUID> idsPadre);

    @Query("select case when count(h) > 0 then true else false end from HistorialSolicitud h where h.solicitud.id_solicitud = :idPadre")
    boolean existsByPadre(@Param("idPadre") UUID idPadre);

    // KPI SLA de despacho: primer cambio desde "Pendiente" de cada Solicitud (despacho a OT o RQ).
    @Query("select h.solicitud.id_solicitud, min(h.fecha) from HistorialSolicitud h "
            + "where h.estado_anterior.nombre = 'Pendiente' and h.estado_nuevo.nombre <> 'Pendiente' "
            + "and h.solicitud.id_solicitud in :idsSolicitud group by h.solicitud.id_solicitud")
    List<Object[]> primerDespacho(@Param("idsSolicitud") Collection<UUID> idsSolicitud);
}
