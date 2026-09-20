package cfbd.co.sgt.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
