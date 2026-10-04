package cfbd.co.sgt.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cfbd.co.sgt.model.UsuarioEspecialidad;

// JPQL explícito: los ids no se llaman "id" (mismo motivo que UsuarioRepository).
@Repository
public interface UsuarioEspecialidadRepository extends JpaRepository<UsuarioEspecialidad, UUID> {

    @Query("select ue from UsuarioEspecialidad ue join fetch ue.usuario u "
            + "where ue.especialidad.id_especialidad = :idEspecialidad order by u.nombres, u.apellidos")
    List<UsuarioEspecialidad> findByEspecialidad(@Param("idEspecialidad") UUID idEspecialidad);

    @Query("select ue from UsuarioEspecialidad ue join fetch ue.especialidad e "
            + "where ue.usuario.id_usuario = :idUsuario order by e.nombre")
    List<UsuarioEspecialidad> findByUsuario(@Param("idUsuario") UUID idUsuario);

    @Query("select case when count(ue) > 0 then true else false end from UsuarioEspecialidad ue "
            + "where ue.usuario.id_usuario = :idUsuario and ue.especialidad.id_especialidad = :idEspecialidad")
    boolean esMiembro(@Param("idUsuario") UUID idUsuario, @Param("idEspecialidad") UUID idEspecialidad);

    @Query("select case when count(ue) > 0 then true else false end from UsuarioEspecialidad ue "
            + "where ue.usuario.id_usuario = :idUsuario and ue.especialidad.id_especialidad = :idEspecialidad "
            + "and ue.es_responsable = true")
    boolean esResponsable(@Param("idUsuario") UUID idUsuario, @Param("idEspecialidad") UUID idEspecialidad);

    @Modifying
    @Query("delete from UsuarioEspecialidad ue where ue.especialidad.id_especialidad = :idEspecialidad")
    void deleteByEspecialidad(@Param("idEspecialidad") UUID idEspecialidad);
}
