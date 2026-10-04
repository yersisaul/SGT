package cfbd.co.sgt.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cfbd.co.sgt.model.Activo;
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.Rol;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.repository.UsuarioEspecialidadRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.security.Permisos;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;

/**
 * El adjunto de Solicitud debe respetar la misma autorización de recurso que
 * ya protege buscarSolicitudPorId: el fileserver no debe convertirse en una puerta para
 * saltarse esa regla.
 */
@ExtendWith(MockitoExtension.class)
class SolicitudServiceImplArchivoTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UsuarioEspecialidadRepository usuarioEspecialidadRepository;

    @Mock
    private cfbd.co.sgt.service.SlaCalculator slaCalculator;

    private SolicitudServiceImpl service;

    private UUID idSolicitud;
    private Solicitud solicitud;
    private Usuario duenio;
    private Usuario otroCliente;
    private Rol rolCliente;

    @BeforeEach
    void setUp() {
        service = new SolicitudServiceImpl();
        ReflectionTestUtils.setField(service, "solicitudRepository", solicitudRepository);
        ReflectionTestUtils.setField(service, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(service, "fileStorageService", fileStorageService);
        ReflectionTestUtils.setField(service, "slaCalculator", slaCalculator);
        UsuarioActualProvider usuarioActual = new UsuarioActualProvider(usuarioRepository);
        ReflectionTestUtils.setField(service, "usuarioActual", usuarioActual);
        ReflectionTestUtils.setField(service, "autorizacion",
                new AutorizacionRecursoServiceImpl(usuarioActual, solicitudRepository, usuarioEspecialidadRepository));

        rolCliente = new Rol();
        rolCliente.setNombre("Cliente");

        duenio = new Usuario();
        duenio.setId_usuario(UUID.randomUUID());
        duenio.setEmail("duenio@cfbd.co");
        duenio.setRol(rolCliente);

        otroCliente = new Usuario();
        otroCliente.setId_usuario(UUID.randomUUID());
        otroCliente.setEmail("otro@cfbd.co");
        otroCliente.setRol(rolCliente);

        idSolicitud = UUID.randomUUID();
        solicitud = new Solicitud();
        solicitud.setId_solicitud(idSolicitud);
        solicitud.setUsuario(duenio);

        Activo activo = new Activo();
        activo.setId_activo(UUID.randomUUID());
        solicitud.setActivo(activo);

        Estado estado = new Estado();
        estado.setId_estado(UUID.randomUUID());
        estado.setNombre("Pendiente");
        solicitud.setEstado(estado);

        Especialidad especialidad = new Especialidad();
        especialidad.setId_especialidad(UUID.randomUUID());
        solicitud.setEspecialidad(especialidad);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario usuario, String... permisos) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario.getEmail(), null, Arrays.stream(permisos).map(SimpleGrantedAuthority::new).toList()));
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
    }

    @Test
    void elDuenioPuedeSubirSuAdjunto() {
        autenticarComo(duenio);
        when(solicitudRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.esVisiblePara(idSolicitud, duenio.getId_usuario())).thenReturn(true);
        when(fileStorageService.store(any(MultipartFile.class), eq(TipoRecursoArchivo.SOLICITUDES), eq(idSolicitud)))
                .thenReturn("solicitudes/" + idSolicitud + "/archivo.pdf");
        when(solicitudRepository.save(solicitud)).thenReturn(solicitud);

        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});
        var response = service.subirAdjunto(idSolicitud, file);

        assertThat(response.getUrl_adjunto()).isEqualTo("/api/archivos/solicitudes/" + idSolicitud);
    }

    @Test
    void otroClienteNoPuedeSubirElAdjuntoDeUnaSolicitudAjena() {
        autenticarComo(otroCliente);
        when(solicitudRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));

        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});
        assertThatThrownBy(() -> service.subirAdjunto(idSolicitud, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void otroClienteNoPuedeDescargarElAdjuntoDeUnaSolicitudAjena() {
        autenticarComo(otroCliente);
        solicitud.setUrl_adjunto("solicitudes/" + idSolicitud + "/archivo.pdf");
        when(solicitudRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.obtenerReferenciaAdjunto(idSolicitud))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void otroClienteNoPuedeEliminarElAdjuntoDeUnaSolicitudAjena() {
        autenticarComo(otroCliente);
        solicitud.setUrl_adjunto("solicitudes/" + idSolicitud + "/archivo.pdf");
        when(solicitudRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.eliminarAdjunto(idSolicitud))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void conAlcanceGlobalPuedeVerCualquierSolicitud() {
        Rol rolDespachador = new Rol();
        rolDespachador.setNombre("Despachador");
        Usuario despachador = new Usuario();
        despachador.setId_usuario(UUID.randomUUID());
        despachador.setEmail("despachador@cfbd.co");
        despachador.setRol(rolDespachador);

        autenticarComo(despachador, Permisos.SOLICITUD_READ_ALL);
        solicitud.setUrl_adjunto("solicitudes/" + idSolicitud + "/archivo.pdf");
        when(solicitudRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));

        assertThat(service.obtenerReferenciaAdjunto(idSolicitud))
                .isEqualTo("solicitudes/" + idSolicitud + "/archivo.pdf");
    }
}
