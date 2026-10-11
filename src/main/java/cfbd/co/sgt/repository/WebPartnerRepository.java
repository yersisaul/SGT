package cfbd.co.sgt.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cfbd.co.sgt.model.WebPartner;

public interface WebPartnerRepository extends JpaRepository<WebPartner, UUID> {

    @Query("select p from WebPartner p order by p.orden, p.nombre")
    List<WebPartner> findAllOrdenados();

    @Query("select p from WebPartner p where p.publicado = true order by p.orden, p.nombre")
    List<WebPartner> findPublicados();

    // JPQL explícito: los exists derivados fallan con ids que no se llaman "id" (ver UsuarioRepository).
    @Query("select case when count(p) > 0 then true else false end from WebPartner p where p.nombre = :nombre")
    boolean existsByNombre(@Param("nombre") String nombre);
}
