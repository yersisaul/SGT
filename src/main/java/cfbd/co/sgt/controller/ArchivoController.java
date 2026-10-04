package cfbd.co.sgt.controller;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.response.ActivoResponse;
import cfbd.co.sgt.dto.response.AprobacionResponse;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.dto.response.RequerimientoResponse;
import cfbd.co.sgt.dto.response.SolicitudResponse;
import cfbd.co.sgt.dto.response.UsuarioResponse;
import cfbd.co.sgt.service.ActivoService;
import cfbd.co.sgt.service.AprobacionService;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.OrdenService;
import cfbd.co.sgt.service.RequerimientoService;
import cfbd.co.sgt.service.SolicitudService;
import cfbd.co.sgt.service.UsuarioService;
import lombok.RequiredArgsConstructor;

/**
 * Fileserver propio del backend SGT: sirve y recibe los archivos de las 6
 * entidades que aceptan imagen/adjunto (Activo, Usuario, Solicitud,
 * Requerimiento, Aprobacion, Orden). El almacenamiento físico y la
 * validación de archivo viven en FileStorageService; la autorización de
 * permiso (@PreAuthorize) y de recurso (puedeVer) vive en el Service de cada
 * entidad — este Controller solo enruta, reutilizando exactamente los mismos
 * permisos que ya protegen el CRUD de cada recurso.
 */
@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
public class ArchivoController {

    private final ActivoService activoService;
    private final UsuarioService usuarioService;
    private final SolicitudService solicitudService;
    private final RequerimientoService requerimientoService;
    private final AprobacionService aprobacionService;
    private final OrdenService ordenService;
    private final FileStorageService fileStorageService;

    // ---- Activos (imagen) ----
    @PreAuthorize("hasAuthority('activo.update')")
    @PostMapping("/activos/{id}")
    public ResponseEntity<ActivoResponse> subirImagenActivo(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(activoService.subirImagen(id, file));
    }

    @PreAuthorize("hasAuthority('activo.read')")
    @GetMapping("/activos/{id}")
    public ResponseEntity<Resource> descargarImagenActivo(@PathVariable UUID id) {
        return construirDescarga(activoService.obtenerReferenciaImagen(id));
    }

    @PreAuthorize("hasAuthority('activo.update')")
    @DeleteMapping("/activos/{id}")
    public ResponseEntity<ActivoResponse> eliminarImagenActivo(@PathVariable UUID id) {
        return ResponseEntity.ok(activoService.eliminarImagen(id));
    }

    // ---- Usuarios (foto) ----
    @PreAuthorize("hasAuthority('usuario.update')")
    @PostMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponse> subirFotoUsuario(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(usuarioService.subirImagen(id, file));
    }

    @PreAuthorize("hasAuthority('usuario.read')")
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<Resource> descargarFotoUsuario(@PathVariable UUID id) {
        return construirDescarga(usuarioService.obtenerReferenciaImagen(id));
    }

    @PreAuthorize("hasAuthority('usuario.update')")
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponse> eliminarFotoUsuario(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.eliminarImagen(id));
    }

    // ---- Solicitudes (adjunto) ----
    @PreAuthorize("hasAuthority('solicitud.update')")
    @PostMapping("/solicitudes/{id}")
    public ResponseEntity<SolicitudResponse> subirAdjuntoSolicitud(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(solicitudService.subirAdjunto(id, file));
    }

    @PreAuthorize("hasAuthority('solicitud.read')")
    @GetMapping("/solicitudes/{id}")
    public ResponseEntity<Resource> descargarAdjuntoSolicitud(@PathVariable UUID id) {
        return construirDescarga(solicitudService.obtenerReferenciaAdjunto(id));
    }

    @PreAuthorize("hasAuthority('solicitud.update')")
    @DeleteMapping("/solicitudes/{id}")
    public ResponseEntity<SolicitudResponse> eliminarAdjuntoSolicitud(@PathVariable UUID id) {
        return ResponseEntity.ok(solicitudService.eliminarAdjunto(id));
    }

    // ---- Requerimientos (adjunto) ----
    @PreAuthorize("hasAuthority('requerimiento.update')")
    @PostMapping("/requerimientos/{id}")
    public ResponseEntity<RequerimientoResponse> subirAdjuntoRequerimiento(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(requerimientoService.subirAdjunto(id, file));
    }

    @PreAuthorize("hasAuthority('requerimiento.read')")
    @GetMapping("/requerimientos/{id}")
    public ResponseEntity<Resource> descargarAdjuntoRequerimiento(@PathVariable UUID id) {
        return construirDescarga(requerimientoService.obtenerReferenciaAdjunto(id));
    }

    @PreAuthorize("hasAuthority('requerimiento.update')")
    @DeleteMapping("/requerimientos/{id}")
    public ResponseEntity<RequerimientoResponse> eliminarAdjuntoRequerimiento(@PathVariable UUID id) {
        return ResponseEntity.ok(requerimientoService.eliminarAdjunto(id));
    }

    // ---- Aprobaciones (presupuesto/documento) ----
    // Sin permiso aprobacion.update (una Aprobacion es un registro de evento,
    // no editable vía CRUD): se gobierna con requerimiento.aprobar, el mismo
    // permiso que ya protege su creación (ver AprobacionController).

    @PreAuthorize("hasAuthority('requerimiento.aprobar')")
    @PostMapping("/aprobaciones/{id}")
    public ResponseEntity<AprobacionResponse> subirAdjuntoAprobacion(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(aprobacionService.subirAdjunto(id, file));
    }

    @PreAuthorize("hasAuthority('aprobacion.read')")
    @GetMapping("/aprobaciones/{id}")
    public ResponseEntity<Resource> descargarAdjuntoAprobacion(@PathVariable UUID id) {
        return construirDescarga(aprobacionService.obtenerReferenciaAdjunto(id));
    }

    @PreAuthorize("hasAuthority('requerimiento.aprobar')")
    @DeleteMapping("/aprobaciones/{id}")
    public ResponseEntity<AprobacionResponse> eliminarAdjuntoAprobacion(@PathVariable UUID id) {
        return ResponseEntity.ok(aprobacionService.eliminarAdjunto(id));
    }

    // ---- Ordenes (informe técnico / entregable) ----

    @PreAuthorize("hasAuthority('orden.update')")
    @PostMapping("/ordenes/{id}")
    public ResponseEntity<OrdenResponse> subirAdjuntoOrden(@PathVariable UUID id,
                                                             @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ordenService.subirAdjunto(id, file));
    }

    @PreAuthorize("hasAuthority('orden.read')")
    @GetMapping("/ordenes/{id}")
    public ResponseEntity<Resource> descargarAdjuntoOrden(@PathVariable UUID id) {
        return construirDescarga(ordenService.obtenerReferenciaAdjunto(id));
    }

    @PreAuthorize("hasAuthority('orden.update')")
    @DeleteMapping("/ordenes/{id}")
    public ResponseEntity<OrdenResponse> eliminarAdjuntoOrden(@PathVariable UUID id) {
        return ResponseEntity.ok(ordenService.eliminarAdjunto(id));
    }

    private ResponseEntity<Resource> construirDescarga(String referencia) {
        Resource resource = fileStorageService.loadAsResource(referencia);
        String contentType = fileStorageService.detectarContentType(referencia);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
