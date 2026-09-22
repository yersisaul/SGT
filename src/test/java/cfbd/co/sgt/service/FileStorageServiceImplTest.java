package cfbd.co.sgt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.exception.FileStorageException;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.impl.FileStorageServiceImpl;

/**
 * Pruebas unitarias del fileserver propio (sin Spring context): validación
 * de archivo, almacenamiento/reemplazo/eliminación y protección contra path
 * traversal (CLAUDE.md sección 40).
 */
class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private FileStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileStorageServiceImpl(tempDir.toString(), 1, 1);
    }

    private MultipartFile imagenValida() {
        return new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3, 4});
    }

    @Test
    void almacenaUnaImagenValidaYDevuelveReferenciaLogica() {
        UUID id = UUID.randomUUID();
        String referencia = service.store(imagenValida(), TipoRecursoArchivo.ACTIVOS, id);

        assertThat(referencia).isEqualTo("activos/" + id + "/" + referencia.substring(referencia.lastIndexOf('/') + 1));
        assertThat(referencia).startsWith("activos/" + id + "/").endsWith(".png");
        assertThat(Files.exists(tempDir.resolve(referencia))).isTrue();
    }

    @Test
    void rechazaArchivoVacio() {
        MultipartFile vacio = new MockMultipartFile("file", "vacio.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> service.store(vacio, TipoRecursoArchivo.ACTIVOS, UUID.randomUUID()))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void rechazaExtensionNoPermitida() {
        MultipartFile exe = new MockMultipartFile("file", "malware.exe", "application/octet-stream",
                new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.store(exe, TipoRecursoArchivo.ACTIVOS, UUID.randomUUID()))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void rechazaMimeQueNoCoincideConLaCategoria() {
        // pdf no está permitido para IMAGEN (solo para ADJUNTO).
        MultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.store(pdf, TipoRecursoArchivo.ACTIVOS, UUID.randomUUID()))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void aceptaPdfParaAdjuntos() {
        UUID id = UUID.randomUUID();
        MultipartFile pdf = new MockMultipartFile("file", "informe.pdf", "application/pdf", new byte[]{1, 2, 3});
        String referencia = service.store(pdf, TipoRecursoArchivo.ORDENES, id);
        assertThat(referencia).endsWith(".pdf");
    }

    @Test
    void rechazaArchivoQueExcedeElTamanoMaximo() throws IOException {
        byte[] doceMb = new byte[12 * 1024 * 1024];
        MultipartFile grande = new MockMultipartFile("file", "grande.png", "image/png", doceMb);
        assertThatThrownBy(() -> service.store(grande, TipoRecursoArchivo.ACTIVOS, UUID.randomUUID()))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void ignoraCualquierRutaEnElNombreOriginalYUsaSoloLaExtension() {
        UUID id = UUID.randomUUID();
        MultipartFile file = new MockMultipartFile("file", "../../../etc/passwd.png", "image/png",
                new byte[]{1, 2, 3});
        String referencia = service.store(file, TipoRecursoArchivo.ACTIVOS, id);
        // El nombre físico es siempre un UUID propio; el path del original se descarta.
        assertThat(referencia).startsWith("activos/" + id + "/");
        assertThat(referencia).doesNotContain("..");
        assertThat(referencia).doesNotContain("etc");
    }

    @Test
    void reemplazarGuardaElNuevoAntesDeQueSePuedaBorrarElAnterior() {
        UUID id = UUID.randomUUID();
        String referenciaOriginal = service.store(imagenValida(), TipoRecursoArchivo.ACTIVOS, id);
        assertThat(Files.exists(tempDir.resolve(referenciaOriginal))).isTrue();

        String referenciaNueva = service.store(imagenValida(), TipoRecursoArchivo.ACTIVOS, id);
        assertThat(Files.exists(tempDir.resolve(referenciaNueva))).isTrue();
        assertThat(referenciaNueva).isNotEqualTo(referenciaOriginal);
        // El anterior sigue existiendo hasta que el llamador confirme la BD y
        // pida borrarlo explícitamente (CLAUDE.md sección 30).
        assertThat(Files.exists(tempDir.resolve(referenciaOriginal))).isTrue();

        service.delete(referenciaOriginal);
        assertThat(Files.exists(tempDir.resolve(referenciaOriginal))).isFalse();
        assertThat(Files.exists(tempDir.resolve(referenciaNueva))).isTrue();
    }

    @Test
    void eliminarUnaReferenciaInexistenteNoFalla() {
        service.delete("activos/" + UUID.randomUUID() + "/no-existe.png");
    }

    @Test
    void eliminarConReferenciaNulaOVaciaNoFalla() {
        service.delete(null);
        service.delete("");
    }

    @Test
    void cargaUnArchivoAlmacenadoComoResource() throws IOException {
        UUID id = UUID.randomUUID();
        String referencia = service.store(imagenValida(), TipoRecursoArchivo.ACTIVOS, id);

        Resource resource = service.loadAsResource(referencia);
        assertThat(resource.exists()).isTrue();
        assertThat(resource.contentLength()).isEqualTo(4);
    }

    @Test
    void cargarUnaReferenciaInexistenteLanzaResourceNotFound() {
        assertThatThrownBy(() -> service.loadAsResource("activos/" + UUID.randomUUID() + "/no-existe.png"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rechazaReferenciasQueIntentanSalirDelDirectorioRaiz() {
        assertThatThrownBy(() -> service.loadAsResource("../../fuera-del-root.png"))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void detectaElContentTypeDeUnArchivoAlmacenado() {
        UUID id = UUID.randomUUID();
        String referencia = service.store(imagenValida(), TipoRecursoArchivo.ACTIVOS, id);
        assertThat(service.detectarContentType(referencia)).isEqualTo("image/png");
    }
}
