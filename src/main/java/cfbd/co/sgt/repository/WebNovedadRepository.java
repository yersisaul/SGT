package cfbd.co.sgt.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cfbd.co.sgt.model.EstadoNovedad;
import cfbd.co.sgt.model.WebNovedad;

public interface WebNovedadRepository extends JpaRepository<WebNovedad, UUID> {

    // ---- Panel: todas, las más recientes primero ----

    @Query(value = "select n from WebNovedad n left join fetch n.categoria left join fetch n.autor "
            + "order by n.fecha_creacion desc",
            countQuery = "select count(n) from WebNovedad n")
    Page<WebNovedad> findTodas(Pageable pageable);

    @Query(value = "select n from WebNovedad n left join fetch n.categoria left join fetch n.autor "
            + "where n.estado = :estado order by n.fecha_creacion desc",
            countQuery = "select count(n) from WebNovedad n where n.estado = :estado")
    Page<WebNovedad> findPorEstado(@Param("estado") EstadoNovedad estado, Pageable pageable);

    // ---- Sitio: publicadas cuya fecha de publicación ya llegó ----

    @Query(value = "select n from WebNovedad n left join fetch n.categoria left join fetch n.autor "
            + "where n.estado = :estado and n.fecha_publicacion <= :ahora "
            + "order by n.destacado desc, n.fecha_publicacion desc",
            countQuery = "select count(n) from WebNovedad n where n.estado = :estado and n.fecha_publicacion <= :ahora")
    Page<WebNovedad> findVisibles(@Param("estado") EstadoNovedad estado, @Param("ahora") Instant ahora,
            Pageable pageable);

    @Query(value = "select n from WebNovedad n join fetch n.categoria c left join fetch n.autor "
            + "where n.estado = :estado and n.fecha_publicacion <= :ahora and c.slug = :categoria "
            + "order by n.destacado desc, n.fecha_publicacion desc",
            countQuery = "select count(n) from WebNovedad n join n.categoria c "
                    + "where n.estado = :estado and n.fecha_publicacion <= :ahora and c.slug = :categoria")
    Page<WebNovedad> findVisiblesPorCategoria(@Param("estado") EstadoNovedad estado, @Param("ahora") Instant ahora,
            @Param("categoria") String categoria, Pageable pageable);

    @Query("select n from WebNovedad n left join fetch n.categoria left join fetch n.autor where n.slug = :slug")
    Optional<WebNovedad> findBySlug(@Param("slug") String slug);

    // JPQL explícito: los exists derivados fallan con ids que no se llaman "id" (ver UsuarioRepository).
    @Query("select case when count(n) > 0 then true else false end from WebNovedad n where n.slug = :slug")
    boolean existsBySlug(@Param("slug") String slug);

    @Query("select case when count(n) > 0 then true else false end from WebNovedad n "
            + "where n.slug = :slug and n.id_novedad <> :id")
    boolean existsBySlugEnOtra(@Param("slug") String slug, @Param("id") UUID id);
}
