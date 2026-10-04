package cfbd.co.sgt.repository;

import java.util.Collection;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.Orden;
import java.util.UUID;
import java.util.List;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, UUID> {
    // Buscar orden por número de orden
    Optional<Orden> findByNumeroOrden(String numeroOrden);

    // JPQL explícito (no "existsBy..." derivado): el id de Orden no se llama
    // "id" (ver id_orden), y ese patrón derivado falla en esta versión de
    // Spring Data JPA (mismo motivo documentado en UsuarioRepository).
    @Query("select case when count(o) > 0 then true else false end from Orden o where o.solicitud.id_solicitud = :idSolicitud")
    boolean existsBySolicitud(@Param("idSolicitud") UUID idSolicitud);

    @Query("select case when count(o) > 0 then true else false end from Orden o where o.requerimiento.id_requerimiento = :idRequerimiento")
    boolean existsByRequerimiento(@Param("idRequerimiento") UUID idRequerimiento);

    // Órdenes asignadas a un ejecutor (Orden.usuario). JPQL explícito por el
    // mismo motivo que existsBySolicitud/existsByRequerimiento.
    @Query("select o from Orden o where o.usuario.id_usuario = :idUsuario")
    List<Orden> findByUsuario(@Param("idUsuario") UUID idUsuario);

    // Visibilidad sin orden.read_all (CLAUDE.md 6.5): OT asignadas al actor,
    // OT de las especialidades de las que es miembro y OT originadas por una
    // Solicitud del actor (directa o vía Requerimiento).
    @Query("select distinct o from Orden o left join o.usuario u left join o.solicitud s "
            + "left join o.requerimiento r left join r.solicitud rs where u.id_usuario = :idUsuario "
            + "or s.usuario.id_usuario = :idUsuario or rs.usuario.id_usuario = :idUsuario "
            + "or exists (select ue from UsuarioEspecialidad ue where ue.especialidad = o.especialidad "
            + "and ue.usuario.id_usuario = :idUsuario)")
    List<Orden> findVisiblesPara(@Param("idUsuario") UUID idUsuario);

    // Bloqueo de fila para las operaciones de la cola (tomar, asignar,
    // verificar, reasignar, cerrar): serializa los cambios concurrentes sobre
    // una misma OT (PRD NFR-005).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orden o where o.id_orden = :idOrden")
    Optional<Orden> findByIdParaActualizar(@Param("idOrden") UUID idOrden);

    // Cola: OT abiertas en las especialidades del actor, sin ejecutor y en los
    // estados indicados ("Pendiente" para todos; también "Devuelta" para responsables).
    @Query("select o from Orden o where o.usuario is null and o.fecha_cierre is null "
            + "and o.estado.nombre in :estados and o.especialidad.id_especialidad in :idsEspecialidad "
            + "order by o.fecha_registro")
    List<Orden> findCola(@Param("idsEspecialidad") List<UUID> idsEspecialidad, @Param("estados") List<String> estados);

    // OT abiertas por ejecutor dentro de una especialidad (carga del equipo).
    @Query("select o.usuario.id_usuario, count(o) from Orden o where o.fecha_cierre is null "
            + "and o.usuario is not null and o.especialidad.id_especialidad = :idEspecialidad group by o.usuario.id_usuario")
    List<Object[]> contarAbiertasPorEjecutor(@Param("idEspecialidad") UUID idEspecialidad);

    @Query("select case when count(o) > 0 then true else false end from Orden o left join o.solicitud s "
            + "left join o.requerimiento r left join r.solicitud rs "
            + "where s.id_solicitud = :idSolicitud or rs.id_solicitud = :idSolicitud")
    boolean existsPorSolicitudDirectaOIndirecta(@Param("idSolicitud") UUID idSolicitud);

    // ---- KPIs (PRD E6) ----
    @Query("select o from Orden o where o.fecha_registro >= :desde and o.fecha_registro < :hasta")
    List<Orden> findRegistradasEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("select o from Orden o where o.fecha_registro >= :desde and o.fecha_registro < :hasta "
            + "and o.especialidad.id_especialidad in :idsEspecialidad")
    List<Orden> findRegistradasEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
                                     @Param("idsEspecialidad") Collection<UUID> idsEspecialidad);

    @Query("select o from Orden o where o.fecha_cierre is null")
    List<Orden> findAbiertas();

    @Query("select o from Orden o where o.fecha_cierre is null and o.especialidad.id_especialidad in :idsEspecialidad")
    List<Orden> findAbiertas(@Param("idsEspecialidad") Collection<UUID> idsEspecialidad);

    // Escalado (decisión 2026-10-04): OT en cola, sin ejecutor y aún sin tomar.
    @Query("select o from Orden o where o.usuario is null and o.fecha_cierre is null and o.estado.nombre = 'Pendiente'")
    List<Orden> findEnColaSinTomar();

    // KPI SLA de atención: OT generadas directamente desde estas Solicitudes (bajo contrato).
    @Query("select o from Orden o where o.solicitud.id_solicitud in :idsSolicitud")
    List<Orden> findBySolicitudes(@Param("idsSolicitud") Collection<UUID> idsSolicitud);
}
