package cfbd.co.sgt.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.exception.FileStorageException;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private final Path root;
    private final long imageMaxSizeBytes;
    private final long documentMaxSizeBytes;

    public FileStorageServiceImpl(
            @Value("${app.storage.root:./uploads}") String storageRoot,
            @Value("${app.storage.image-max-size-mb:5}") long imageMaxSizeMb,
            @Value("${app.storage.document-max-size-mb:10}") long documentMaxSizeMb) {
        this.root = Paths.get(storageRoot).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new FileStorageException("No se pudo inicializar el directorio de almacenamiento: " + e.getMessage());
        }
        this.imageMaxSizeBytes = imageMaxSizeMb * 1024 * 1024;
        this.documentMaxSizeBytes = documentMaxSizeMb * 1024 * 1024;
    }

    @Override
    public String store(MultipartFile file, TipoRecursoArchivo tipo, UUID entidadId) {
        validar(file, tipo);
        String extension = extensionDe(file.getOriginalFilename());
        String nombreFisico = UUID.randomUUID() + "." + extension;

        Path directorioEntidad = root.resolve(tipo.getCarpeta()).resolve(entidadId.toString()).normalize();
        if (!directorioEntidad.startsWith(root)) {
            throw new FileStorageException("Ruta de almacenamiento inválida.");
        }

        Path destino = directorioEntidad.resolve(nombreFisico).normalize();
        if (!destino.startsWith(directorioEntidad)) {
            throw new FileStorageException("Nombre de archivo inválido.");
        }

        try {
            Files.createDirectories(directorioEntidad);
            file.transferTo(destino);
        } catch (IOException e) {
            throw new FileStorageException("No se pudo almacenar el archivo: " + e.getMessage());
        }

        return tipo.getCarpeta() + "/" + entidadId + "/" + nombreFisico;
    }

    @Override
    public void delete(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return;
        }
        Path archivo = resolverDentroDeRoot(referencia);
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            // No hay una transacción única entre filesystem y BD si el borrado físico falla, no debe tumbar la operación de
            // negocio que lo originó (reemplazo/eliminación ya confirmados en
            // BD); queda un archivo huérfano documentado aquí.
            log.warn("No se pudo eliminar el archivo físico '{}': {}", referencia, e.getMessage());
        }
    }

    @Override
    public Resource loadAsResource(String referencia) {
        Path archivo = resolverDentroDeRoot(referencia);
        if (!Files.isRegularFile(archivo)) {
            throw new ResourceNotFoundException("Archivo no encontrado.");
        }
        try {
            Resource resource = new UrlResource(archivo.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Archivo no encontrado.");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Archivo no encontrado.");
        }
    }

    @Override
    public String detectarContentType(String referencia) {
        Path archivo = resolverDentroDeRoot(referencia);
        try {
            String tipo = Files.probeContentType(archivo);
            return tipo != null ? tipo : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    //Resuelve una referencia lógica dentro de root, rechazando cualquier intento de escapar del directorio raíz (por ejemplo, "../" o "/etc/passwd"). Si la referencia no está dentro de root, lanza FileStorageException. Esto protege contra ataques de path traversal y asegura que los archivos se almacenen y accedan solo dentro del directorio designado.
    private Path resolverDentroDeRoot(String referencia) {
        Path resuelto = root.resolve(referencia).normalize();
        if (!resuelto.startsWith(root)) {
            throw new FileStorageException("Referencia de archivo inválida.");
        }
        return resuelto;
    }

    private void validar(MultipartFile file, TipoRecursoArchivo tipo) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("El archivo está vacío.");
        }

        String extension = extensionDe(file.getOriginalFilename());
        if (extension.isEmpty() || !tipo.extensionesPermitidas().contains(extension)) {
            throw new FileStorageException("Extensión de archivo no permitida: ." + extension);
        }

        String contentType = file.getContentType();
        if (contentType == null || !tipo.mimesPermitidos().contains(contentType.toLowerCase())) {
            throw new FileStorageException("Tipo de archivo no permitido: " + contentType);
        }

        long maxBytes = tipo.getCategoria() == TipoRecursoArchivo.Categoria.IMAGEN
                ? imageMaxSizeBytes
                : documentMaxSizeBytes;
        if (file.getSize() > maxBytes) {
            throw new FileStorageException(
                    "El archivo excede el tamaño máximo permitido (" + (maxBytes / (1024 * 1024)) + " MB).");
        }
    }

    /** Nunca confía en el nombre original del archivo salvo para leer su
     * extensión; Paths.get(...).getFileName() descarta
     * cualquier componente de ruta que venga en el nombre enviado por el
     * navegador. */
    private String extensionDe(String nombreOriginal) {
        if (nombreOriginal == null) {
            return "";
        }
        String limpio = Paths.get(nombreOriginal).getFileName().toString();
        int idx = limpio.lastIndexOf('.');
        if (idx < 0 || idx == limpio.length() - 1) {
            return "";
        }
        return limpio.substring(idx + 1).toLowerCase();
    }
}
