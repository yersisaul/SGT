package cfbd.co.sgt.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cfbd.co.sgt.model.WebServicio;

public interface WebServicioRepository extends JpaRepository<WebServicio, UUID> {

    @Query("select s from WebServicio s order by s.orden, s.nombre")
    List<WebServicio> findAllOrdenados();

    @Query("select s from WebServicio s where s.publicado = true order by s.orden, s.nombre")
    List<WebServicio> findPublicados();

    @Query("select s from WebServicio s where s.slug = :slug and s.publicado = true")
    Optional<WebServicio> findPublicadoPorSlug(@Param("slug") String slug);

    // JPQL explícito: los exists derivados fallan con ids que no se llaman "id" (ver UsuarioRepository).
    @Query("select case when count(s) > 0 then true else false end from WebServicio s where s.slug = :slug")
    boolean existsBySlug(@Param("slug") String slug);

    @Query("select case when count(s) > 0 then true else false end from WebServicio s "
            + "where s.slug = :slug and s.id_servicio <> :id")
    boolean existsBySlugEnOtro(@Param("slug") String slug, @Param("id") UUID id);
}
