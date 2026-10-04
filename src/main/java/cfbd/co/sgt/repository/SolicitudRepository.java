package cfbd.co.sgt.repository;

import java.util.Collection;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import cfbd.co.sgt.model.Solicitud;
import java.util.UUID;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, UUID> {
    Optional<Solicitud> findByNumeroSolicitud(String numeroSolicitud);

    // Cliente solo puede listar sus propias
    // Solicitudes. JPQL explícito (no "findBy...Usuario_IdUsuario" derivado):
    // el id de Usuario se llama literalmente "id_usuario", y Spring Data no
    // resuelve ese nombre vía el parser de propiedades derivado (mismo tipo
    // de limitación documentado en UsuarioRepository).
    @Query("select s from Solicitud s where s.usuario.id_usuario = :idUsuario")
    List<Solicitud> findByUsuario(@Param("idUsuario") UUID idUsuario);

    @Query("select s.estado.nombre, count(s) from Solicitud s group by s.estado.nombre")
    List<Object[]> countByEstado();

    // Visibilidad sin solicitud.read_all: las propias y las
    // que originaron una OT (directa o vía Requerimiento) asignada al actor o
    // de una especialidad de la que es miembro.
    @Query("select distinct s from Solicitud s where s.usuario.id_usuario = :idUsuario "
            + "or exists (select o from Orden o left join o.usuario u left join o.requerimiento r "
            + "where (o.solicitud = s or r.solicitud = s) and (u.id_usuario = :idUsuario "
            + "or exists (select ue from UsuarioEspecialidad ue where ue.especialidad = o.especialidad "
            + "and ue.usuario.id_usuario = :idUsuario)))")
    List<Solicitud> findVisiblesPara(@Param("idUsuario") UUID idUsuario);

    @Query("select case when count(s) > 0 then true else false end from Solicitud s "
            + "where s.id_solicitud = :idSolicitud and (s.usuario.id_usuario = :idUsuario "
            + "or exists (select o from Orden o left join o.usuario u left join o.requerimiento r "
            + "where (o.solicitud = s or r.solicitud = s) and (u.id_usuario = :idUsuario "
            + "or exists (select ue from UsuarioEspecialidad ue where ue.especialidad = o.especialidad "
            + "and ue.usuario.id_usuario = :idUsuario))))")
    boolean esVisiblePara(@Param("idSolicitud") UUID idSolicitud, @Param("idUsuario") UUID idUsuario);

    // ---- KPIs (PRD E6): Solicitudes registradas en un rango ----
    @Query("select s from Solicitud s where s.fecha_registro >= :desde and s.fecha_registro < :hasta")
    List<Solicitud> findRegistradasEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("select s from Solicitud s where s.fecha_registro >= :desde and s.fecha_registro < :hasta "
            + "and s.especialidad.id_especialidad in :idsEspecialidad")
    List<Solicitud> findRegistradasEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
                                         @Param("idsEspecialidad") Collection<UUID> idsEspecialidad);
}
