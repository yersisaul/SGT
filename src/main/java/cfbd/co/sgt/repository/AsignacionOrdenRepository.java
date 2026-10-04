package cfbd.co.sgt.repository;

import cfbd.co.sgt.model.TipoAsignacionOrden;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cfbd.co.sgt.model.AsignacionOrden;

@Repository
public interface AsignacionOrdenRepository extends JpaRepository<AsignacionOrden, UUID> {

    @Query("select a from AsignacionOrden a where a.orden.id_orden = :idOrden order by a.fecha")
    List<AsignacionOrden> findByOrden(@Param("idOrden") UUID idOrden);

    @Query("select a from AsignacionOrden a where a.orden.id_orden in :idsOrden order by a.fecha")
    List<AsignacionOrden> findByOrdenes(@Param("idsOrden") Collection<UUID> idsOrden);

    /** Última vez que cada OT entró a la cola (ENCOLADA o REASIGNADA_ESPECIALIDAD). */
    @Query("select a.orden.id_orden, max(a.fecha) from AsignacionOrden a where a.orden.id_orden in :idsOrden "
            + "and a.tipo in :tipos group by a.orden.id_orden")
    List<Object[]> ultimaFechaPorTipo(@Param("idsOrden") Collection<UUID> idsOrden,
                                      @Param("tipos") Collection<TipoAsignacionOrden> tipos);
}
