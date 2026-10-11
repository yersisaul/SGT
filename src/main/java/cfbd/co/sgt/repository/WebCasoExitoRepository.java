package cfbd.co.sgt.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cfbd.co.sgt.model.WebCasoExito;

public interface WebCasoExitoRepository extends JpaRepository<WebCasoExito, UUID> {

    @Query("select c from WebCasoExito c left join fetch c.cliente left join fetch c.producto "
            + "order by c.orden, c.titulo")
    List<WebCasoExito> findAllOrdenados();

    @Query("select c from WebCasoExito c left join fetch c.cliente left join fetch c.producto "
            + "where c.publicado = true order by c.orden, c.titulo")
    List<WebCasoExito> findPublicados();

    @Query("select c from WebCasoExito c left join fetch c.cliente left join fetch c.producto "
            + "where c.slug = :slug and c.publicado = true")
    Optional<WebCasoExito> findPublicadoPorSlug(@Param("slug") String slug);

    // JPQL explícito: los exists derivados fallan con ids que no se llaman "id" (ver UsuarioRepository).
    @Query("select case when count(c) > 0 then true else false end from WebCasoExito c where c.slug = :slug")
    boolean existsBySlug(@Param("slug") String slug);

    @Query("select case when count(c) > 0 then true else false end from WebCasoExito c "
            + "where c.slug = :slug and c.id_caso_exito <> :id")
    boolean existsBySlugEnOtro(@Param("slug") String slug, @Param("id") UUID id);
}
