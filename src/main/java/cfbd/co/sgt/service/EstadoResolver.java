package cfbd.co.sgt.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.repository.EstadoRepository;
import lombok.RequiredArgsConstructor;

/** Busca un estado del catálogo por nombre; si falta, es un error de configuración (500). */
@Component
@RequiredArgsConstructor
public class EstadoResolver {

    private final EstadoRepository estadoRepository;

    public Estado porNombre(String nombre) {
        return estadoRepository.findByNombre(nombre)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Estado '" + nombre + "' no está configurado en el catálogo."));
    }

    public static boolean es(Estado estado, String nombre) {
        return estado != null && nombre.equalsIgnoreCase(estado.getNombre());
    }
}
