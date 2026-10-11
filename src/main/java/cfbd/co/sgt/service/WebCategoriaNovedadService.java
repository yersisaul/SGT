package cfbd.co.sgt.service;

import java.util.List;
import java.util.UUID;

import cfbd.co.sgt.dto.request.WebCategoriaNovedadRequest;
import cfbd.co.sgt.dto.response.WebCategoriaNovedadResponse;
import cfbd.co.sgt.dto.response.WebNovedadPublicoResponse;

public interface WebCategoriaNovedadService {

    // ---- Panel de administración ----
    List<WebCategoriaNovedadResponse> listar();
    WebCategoriaNovedadResponse crear(WebCategoriaNovedadRequest request);
    WebCategoriaNovedadResponse editar(UUID id, WebCategoriaNovedadRequest request);
    /** 409 si alguna novedad la usa. */
    void eliminar(UUID id);

    // ---- Sitio web público ----
    List<WebNovedadPublicoResponse.CategoriaEnlace> listarPublicas();
}
