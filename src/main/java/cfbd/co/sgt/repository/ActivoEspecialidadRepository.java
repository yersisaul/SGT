package cfbd.co.sgt.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cfbd.co.sgt.model.ActivoEspecialidad;

// JPQL explícito: los ids no se llaman "id" (mismo motivo que UsuarioRepository).
@Repository
public interface ActivoEspecialidadRepository extends JpaRepository<ActivoEspecialidad, UUID> {

    @Query("select ae.especialidad.id_especialidad from ActivoEspecialidad ae where ae.activo.id_activo = :idActivo")
    List<UUID> findIdsEspecialidad(@Param("idActivo") UUID idActivo);

    /** Pares (id_activo, id_especialidad) de todos los activos: evita N consultas al listar. */
    @Query("select ae.activo.id_activo, ae.especialidad.id_especialidad from ActivoEspecialidad ae")
    List<Object[]> findTodosLosPares();

    @Modifying
    @Query("delete from ActivoEspecialidad ae where ae.activo.id_activo = :idActivo")
    void deleteByActivo(@Param("idActivo") UUID idActivo);
}
