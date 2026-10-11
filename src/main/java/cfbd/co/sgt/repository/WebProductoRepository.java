package cfbd.co.sgt.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cfbd.co.sgt.model.WebProducto;

public interface WebProductoRepository extends JpaRepository<WebProducto, UUID> {

    @Query("select p from WebProducto p order by p.orden, p.nombre")
    List<WebProducto> findAllOrdenados();

    @Query("select p from WebProducto p where p.publicado = true order by p.orden, p.nombre")
    List<WebProducto> findPublicados();

    @Query("select p from WebProducto p where p.slug = :slug")
    Optional<WebProducto> findBySlug(@Param("slug") String slug);

    @Query("select p from WebProducto p where p.slug = :slug and p.publicado = true")
    Optional<WebProducto> findPublicadoPorSlug(@Param("slug") String slug);

    // JPQL explícito: los exists derivados fallan con ids que no se llaman "id" (ver UsuarioRepository).
    @Query("select case when count(p) > 0 then true else false end from WebProducto p where p.slug = :slug")
    boolean existsBySlug(@Param("slug") String slug);

    @Query("select case when count(p) > 0 then true else false end from WebProducto p "
            + "where p.slug = :slug and p.id_producto <> :id")
    boolean existsBySlugEnOtro(@Param("slug") String slug, @Param("id") UUID id);
}
