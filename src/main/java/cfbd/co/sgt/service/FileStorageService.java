package cfbd.co.sgt.service;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Fileserver propio del backend: almacena y sirve archivos en el filesystem
 * local (sin MinIO/S3). La BD solo conserva la referencia lógica devuelta por
 * {@link #store}, nunca el binario.
 */
public interface FileStorageService {

    /**
     * Valida y almacena el archivo bajo {@code <root>/<tipo>/<entidadId>/}
     * con un nombre físico generado (UUID + extensión validada). Devuelve la
     * referencia lógica relativa (p.ej. "activos/&lt;uuid&gt;/&lt;archivo&gt;")
     * que debe persistirse en la entidad.
     */
    String store(MultipartFile file, TipoRecursoArchivo tipo, UUID entidadId);

    /** Elimina el archivo físico referenciado. No falla si ya no existe. */
    void delete(String referencia);

    /** Carga el archivo referenciado como Resource para servirlo por HTTP. */
    Resource loadAsResource(String referencia);

    /** Determina el Content-Type del archivo referenciado. */
    String detectarContentType(String referencia);
}
