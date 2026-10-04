package cfbd.co.sgt.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Collection;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import cfbd.co.sgt.model.Aprobacion;
import java.util.UUID;

@Repository 
public interface AprobacionRepository extends JpaRepository<Aprobacion, UUID>  {


    // KPI de ruteo: decisiones del Administrador sobre RQ dentro de un rango.
    @Query("select a from Aprobacion a where a.fecha_aprobacion >= :desde and a.fecha_aprobacion < :hasta")
    List<Aprobacion> findDecididasEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("select a from Aprobacion a where a.fecha_aprobacion >= :desde and a.fecha_aprobacion < :hasta "
            + "and a.requerimiento.especialidad.id_especialidad in :idsEspecialidad")
    List<Aprobacion> findDecididasEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
                                        @Param("idsEspecialidad") Collection<UUID> idsEspecialidad);

    // KPI decisión de RQ: primera decisión del Administrador por Requerimiento.
    @Query("select a.requerimiento.id_requerimiento, min(a.fecha_aprobacion) from Aprobacion a "
            + "where a.requerimiento.id_requerimiento in :idsRequerimiento group by a.requerimiento.id_requerimiento")
    List<Object[]> primeraDecision(@Param("idsRequerimiento") Collection<UUID> idsRequerimiento);
}
