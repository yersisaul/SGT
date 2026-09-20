package cfbd.co.sgt.repository;

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

    // Ownership (CLAUDE.md 5.5): Cliente solo puede listar sus propias
    // Solicitudes. JPQL explícito (no "findBy...Usuario_IdUsuario" derivado):
    // el id de Usuario se llama literalmente "id_usuario", y Spring Data no
    // resuelve ese nombre vía el parser de propiedades derivado (mismo tipo
    // de limitación documentado en UsuarioRepository).
    @Query("select s from Solicitud s where s.usuario.id_usuario = :idUsuario")
    List<Solicitud> findByUsuario(@Param("idUsuario") UUID idUsuario);

    @Query("select s.estado.nombre, count(s) from Solicitud s group by s.estado.nombre")
    List<Object[]> countByEstado();
}
