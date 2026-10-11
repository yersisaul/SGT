package cfbd.co.sgt.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cfbd.co.sgt.model.WebDocumento;

public interface WebDocumentoRepository extends JpaRepository<WebDocumento, UUID> {

    @Query("select d from WebDocumento d left join fetch d.producto order by d.orden, d.titulo")
    List<WebDocumento> findAllOrdenados();

    // Publicado y con archivo: lo único que el sitio puede descargar.
    @Query("select d from WebDocumento d left join fetch d.producto "
            + "where d.publicado = true and d.ruta_archivo is not null order by d.orden, d.titulo")
    List<WebDocumento> findPublicados();

    // JPQL explícito: los exists derivados fallan con ids que no se llaman "id" (ver UsuarioRepository).
    @Query("select case when count(d) > 0 then true else false end from WebDocumento d where d.titulo = :titulo")
    boolean existsByTitulo(@Param("titulo") String titulo);
}
