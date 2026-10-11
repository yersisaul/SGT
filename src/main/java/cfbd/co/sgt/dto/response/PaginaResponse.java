package cfbd.co.sgt.dto.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Página de resultados con un formato JSON estable (no se serializa Page de Spring). */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long total_elementos,
        int total_paginas) {

    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
