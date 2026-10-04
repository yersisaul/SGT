package cfbd.co.sgt.repository;

import java.util.Collection;
import java.time.Instant;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import cfbd.co.sgt.model.Requerimiento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequerimientoRepository extends  JpaRepository<Requerimiento, UUID>  {
    Optional<Requerimiento> findByNumeroRequerimiento(String numeroRequerimiento);

    @Query("select r.estado.nombre, count(r) from Requerimiento r group by r.estado.nombre")
    List<Object[]> countByEstado();

    // Visibilidad sin requerimiento.read_all (CLAUDE.md 6.5): los creados por
    // el actor, los originados en sus Solicitudes y los que tienen una OT
    // asignada al actor.
    @Query("select distinct r from Requerimiento r left join r.solicitud s where r.usuario.id_usuario = :idUsuario "
            + "or s.usuario.id_usuario = :idUsuario "
            + "or exists (select o from Orden o left join o.usuario u where o.requerimiento = r "
            + "and (u.id_usuario = :idUsuario or exists (select ue from UsuarioEspecialidad ue "
            + "where ue.especialidad = o.especialidad and ue.usuario.id_usuario = :idUsuario)))")
    List<Requerimiento> findVisiblesPara(@Param("idUsuario") UUID idUsuario);

    @Query("select case when count(r) > 0 then true else false end from Requerimiento r where r.solicitud.id_solicitud = :idSolicitud")
    boolean existsBySolicitud(@Param("idSolicitud") UUID idSolicitud);

    // KPI SLA de atención: Solicitudes que fueron fuera de contrato (generaron RQ).
    @Query("select r.solicitud.id_solicitud from Requerimiento r where r.solicitud.id_solicitud in :idsSolicitud")
    List<UUID> findIdsSolicitudConRequerimiento(@Param("idsSolicitud") Collection<UUID> idsSolicitud);

    // KPI decisión de RQ: Requerimientos registrados en un rango.
    @Query("select r from Requerimiento r where r.fecha_registro >= :desde and r.fecha_registro < :hasta")
    List<Requerimiento> findRegistradosEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("select r from Requerimiento r where r.fecha_registro >= :desde and r.fecha_registro < :hasta "
            + "and r.especialidad.id_especialidad in :idsEspecialidad")
    List<Requerimiento> findRegistradosEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
                                             @Param("idsEspecialidad") Collection<UUID> idsEspecialidad);
}
