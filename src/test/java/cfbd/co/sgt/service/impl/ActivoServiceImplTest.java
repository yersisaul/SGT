package cfbd.co.sgt.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.response.ActivoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Activo;
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.repository.ActivoRepository;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;

/**
 * Cubre la gestión de imagen de Activo a nivel de Service (CLAUDE.md sección
 * 40: subir, reemplazar, eliminar, recurso inexistente, operación sin
 * archivo) sin necesitar la BD real ni el contexto de Spring.
 */
@ExtendWith(MockitoExtension.class)
class ActivoServiceImplTest {

    @Mock
    private ActivoRepository activoRepository;

    @Mock
    private FileStorageService fileStorageService;

    private ActivoServiceImpl service;

    private UUID id;
    private Activo activo;

    @BeforeEach
    void setUp() {
        service = new ActivoServiceImpl();
        ReflectionTestUtils.setField(service, "activoRepository", activoRepository);
        ReflectionTestUtils.setField(service, "fileStorageService", fileStorageService);

        id = UUID.randomUUID();
        activo = new Activo();
        activo.setId_activo(id);
        Especialidad especialidad = new Especialidad();
        especialidad.setId_especialidad(UUID.randomUUID());
        activo.setEspecialidad(especialidad);
    }

    @Test
    void subirImagenAlmacenaElArchivoYActualizaLaReferencia() {
        when(activoRepository.findById(id)).thenReturn(Optional.of(activo));
        when(fileStorageService.store(any(MultipartFile.class), eq(TipoRecursoArchivo.ACTIVOS), eq(id)))
                .thenReturn("activos/" + id + "/nueva.png");
        when(activoRepository.save(activo)).thenReturn(activo);

        MultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1});
        ActivoResponse response = service.subirImagen(id, file);

        assertThat(activo.getUrl_img()).isEqualTo("activos/" + id + "/nueva.png");
        assertThat(response.getUrl_img()).isEqualTo("/api/archivos/activos/" + id);
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void subirImagenSobreUnActivoConImagenPreviaBorraLaAnteriorDespuesDeGuardar() {
        activo.setUrl_img("activos/" + id + "/vieja.png");
        when(activoRepository.findById(id)).thenReturn(Optional.of(activo));
        when(fileStorageService.store(any(MultipartFile.class), eq(TipoRecursoArchivo.ACTIVOS), eq(id)))
                .thenReturn("activos/" + id + "/nueva.png");
        when(activoRepository.save(activo)).thenReturn(activo);

        MultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1});
        service.subirImagen(id, file);

        verify(fileStorageService).delete("activos/" + id + "/vieja.png");
    }

    @Test
    void subirImagenSobreActivoInexistenteLanzaResourceNotFound() {
        when(activoRepository.findById(id)).thenReturn(Optional.empty());
        MultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1});
        assertThatThrownBy(() -> service.subirImagen(id, file)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerReferenciaImagenSinImagenLanzaResourceNotFound() {
        when(activoRepository.findById(id)).thenReturn(Optional.of(activo));
        assertThatThrownBy(() -> service.obtenerReferenciaImagen(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerReferenciaImagenDeActivoInexistenteLanzaResourceNotFound() {
        when(activoRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtenerReferenciaImagen(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminarImagenBorraElArchivoYLimpiaLaReferencia() {
        activo.setUrl_img("activos/" + id + "/foto.png");
        when(activoRepository.findById(id)).thenReturn(Optional.of(activo));
        when(activoRepository.save(activo)).thenReturn(activo);

        ActivoResponse response = service.eliminarImagen(id);

        verify(fileStorageService).delete("activos/" + id + "/foto.png");
        assertThat(activo.getUrl_img()).isNull();
        assertThat(response.getUrl_img()).isNull();
    }

    @Test
    void eliminarImagenSinImagenNoLlamaAlStorageNiFalla() {
        when(activoRepository.findById(id)).thenReturn(Optional.of(activo));

        service.eliminarImagen(id);

        verify(fileStorageService, never()).delete(any());
    }
}
