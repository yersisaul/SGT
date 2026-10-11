package cfbd.co.sgt.mapper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.WebCategoriaNovedadResponse;
import cfbd.co.sgt.dto.response.WebNovedadPublicoResponse;
import cfbd.co.sgt.dto.response.WebNovedadResponse;
import cfbd.co.sgt.model.WebCategoriaNovedad;
import cfbd.co.sgt.model.WebNovedad;

/** Novedades y sus categorías. */
@Component
public class WebNovedadMapper {

    public WebNovedadResponse toResponse(WebNovedad novedad) {
        WebNovedadResponse response = new WebNovedadResponse();
        response.setId_novedad(novedad.getId_novedad());
        response.setSlug(novedad.getSlug());
        response.setTitulo(novedad.getTitulo());
        response.setResumen(novedad.getResumen());
        response.setContenido(novedad.getContenido());
        response.setUrl_portada(novedad.getUrl_portada() != null
                ? WebMapperSoporte.imagenAdmin("novedades", novedad.getId_novedad(), novedad.getFecha_actualizacion())
                : null);
        response.setEstado(novedad.getEstado());
        response.setFecha_publicacion(novedad.getFecha_publicacion());
        response.setVisible_en_sitio(novedad.esVisibleEn(Instant.now()));
        WebCategoriaNovedad categoria = novedad.getCategoria();
        if (categoria != null) {
            response.setId_categoria_novedad(categoria.getId_categoria_novedad());
            response.setNombre_categoria(categoria.getNombre());
        }
        response.setNombre_autor(WebMapperSoporte.nombreCompleto(novedad.getAutor()));
        response.setDestacado(novedad.isDestacado());
        response.setFecha_creacion(novedad.getFecha_creacion());
        response.setFecha_actualizacion(novedad.getFecha_actualizacion());
        response.setNombre_usuario_actualiza(WebMapperSoporte.nombreCompleto(novedad.getUsuario_actualiza()));
        return response;
    }

    /** {@code conContenido} false para los listados: el texto completo va solo en el detalle. */
    public WebNovedadPublicoResponse toPublicoResponse(WebNovedad novedad, boolean conContenido) {
        WebNovedadPublicoResponse response = new WebNovedadPublicoResponse();
        response.setSlug(novedad.getSlug());
        response.setTitulo(novedad.getTitulo());
        response.setResumen(novedad.getResumen());
        response.setContenido(conContenido ? novedad.getContenido() : null);
        response.setUrl_portada(novedad.getUrl_portada() != null
                ? WebMapperSoporte.imagenPublica("novedades", novedad.getId_novedad(), novedad.getFecha_actualizacion())
                : null);
        response.setFecha_publicacion(novedad.getFecha_publicacion());
        if (novedad.getCategoria() != null) {
            response.setCategoria(toCategoriaEnlace(novedad.getCategoria()));
        }
        response.setAutor(WebMapperSoporte.nombreCompleto(novedad.getAutor()));
        response.setDestacado(novedad.isDestacado());
        return response;
    }

    public WebCategoriaNovedadResponse toCategoriaResponse(WebCategoriaNovedad categoria) {
        WebCategoriaNovedadResponse response = new WebCategoriaNovedadResponse();
        response.setId_categoria_novedad(categoria.getId_categoria_novedad());
        response.setNombre(categoria.getNombre());
        response.setSlug(categoria.getSlug());
        response.setOrden(categoria.getOrden());
        return response;
    }

    public WebNovedadPublicoResponse.CategoriaEnlace toCategoriaEnlace(WebCategoriaNovedad categoria) {
        return new WebNovedadPublicoResponse.CategoriaEnlace(categoria.getSlug(), categoria.getNombre());
    }
}
