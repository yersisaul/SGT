package cfbd.co.sgt.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cfbd.co.sgt.model.WebClienteReferencia;

public interface WebClienteReferenciaRepository extends JpaRepository<WebClienteReferencia, UUID> {

    @Query("select c from WebClienteReferencia c order by c.orden, c.nombre")
    List<WebClienteReferencia> findAllOrdenados();

    @Query("select c from WebClienteReferencia c where c.publicado = true order by c.orden, c.nombre")
    List<WebClienteReferencia> findPublicados();

    @Query("select c from WebClienteReferencia c where c.nombre = :nombre")
    Optional<WebClienteReferencia> findByNombre(@Param("nombre") String nombre);
}
