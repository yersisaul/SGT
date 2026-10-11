package cfbd.co.sgt.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cfbd.co.sgt.model.WebCategoriaNovedad;

public interface WebCategoriaNovedadRepository extends JpaRepository<WebCategoriaNovedad, UUID> {

    @Query("select c from WebCategoriaNovedad c order by c.orden, c.nombre")
    List<WebCategoriaNovedad> findAllOrdenadas();

    // JPQL explícito: los exists derivados fallan con ids que no se llaman "id" (ver UsuarioRepository).
    @Query("select case when count(c) > 0 then true else false end from WebCategoriaNovedad c where c.slug = :slug")
    boolean existsBySlug(@Param("slug") String slug);

    @Query("select case when count(c) > 0 then true else false end from WebCategoriaNovedad c "
            + "where c.slug = :slug and c.id_categoria_novedad <> :id")
    boolean existsBySlugEnOtra(@Param("slug") String slug, @Param("id") UUID id);
}
