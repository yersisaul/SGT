package cfbd.co.sgt.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import cfbd.co.sgt.service.FileStorageService;

/** Respuesta HTTP para mostrar en el navegador un archivo del sitio web. */
final class WebArchivoRespuesta {

    private WebArchivoRespuesta() {
    }

    static ResponseEntity<Resource> inline(FileStorageService fileStorageService, String referencia,
            CacheControl cacheControl) {
        Resource resource = fileStorageService.loadAsResource(referencia);
        String contentType = fileStorageService.detectarContentType(referencia);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(cacheControl)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * PDF que el navegador abre en su visor y, al guardarlo, toma
     * {@code nombre} (p. ej. "CFBD_BROCHURE AZOR ANALYTICS.pdf") en vez del
     * UUID físico. El tipo es fijo: WEB_DOCUMENTOS solo acepta PDF.
     */
    static ResponseEntity<Resource> pdf(FileStorageService fileStorageService, String referencia, String nombre,
            CacheControl cacheControl) {
        Resource resource = fileStorageService.loadAsResource(referencia);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(nombre != null ? nombre : "documento.pdf", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(cacheControl)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
