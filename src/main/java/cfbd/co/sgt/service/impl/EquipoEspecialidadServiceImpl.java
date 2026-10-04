package cfbd.co.sgt.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cfbd.co.sgt.dto.request.EquipoEspecialidadRequest;
import cfbd.co.sgt.dto.request.MiembroEspecialidadRequest;
import cfbd.co.sgt.dto.response.MiEspecialidadResponse;
import cfbd.co.sgt.dto.response.MiembroEspecialidadResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.model.UsuarioEspecialidad;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.repository.UsuarioEspecialidadRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.EquipoEspecialidadService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EquipoEspecialidadServiceImpl implements EquipoEspecialidadService {

    /** Solo puede integrar un equipo quien puede ejecutar OT (permiso, no nombre de rol). */
    private static final String PERMISO_MIEMBRO = "orden.tomar";

    private final UsuarioEspecialidadRepository usuarioEspecialidadRepository;
    private final EspecialidadRepository especialidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioActualProvider usuarioActual;

    @Override
    @Transactional(readOnly = true)
    public List<MiembroEspecialidadResponse> listarMiembros(UUID idEspecialidad) {
        buscarEspecialidad(idEspecialidad);
        return usuarioEspecialidadRepository.findByEspecialidad(idEspecialidad).stream()
                .map(this::toMiembroResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<MiembroEspecialidadResponse> reemplazarMiembros(UUID idEspecialidad, EquipoEspecialidadRequest request) {
        Especialidad especialidad = buscarEspecialidad(idEspecialidad);
        validarSinDuplicados(request.getMiembros());
        List<UsuarioEspecialidad> nuevos = request.getMiembros().stream()
                .map(miembro -> crearPertenencia(especialidad, miembro))
                .toList();
        usuarioEspecialidadRepository.deleteByEspecialidad(idEspecialidad);
        usuarioEspecialidadRepository.flush();
        usuarioEspecialidadRepository.saveAll(nuevos);
        return listarMiembros(idEspecialidad);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MiEspecialidadResponse> listarMisEspecialidades() {
        return usuarioEspecialidadRepository.findByUsuario(usuarioActual.obtener().getId_usuario()).stream()
                .map(this::toMiEspecialidadResponse)
                .toList();
    }

    private Especialidad buscarEspecialidad(UUID idEspecialidad) {
        return especialidadRepository.findById(idEspecialidad)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found"));
    }

    private void validarSinDuplicados(List<MiembroEspecialidadRequest> miembros) {
        Set<UUID> vistos = new HashSet<>();
        for (MiembroEspecialidadRequest miembro : miembros) {
            if (!vistos.add(miembro.getId_usuario())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El usuario " + miembro.getId_usuario() + " está repetido en el equipo.");
            }
        }
    }

    private UsuarioEspecialidad crearPertenencia(Especialidad especialidad, MiembroEspecialidadRequest miembro) {
        Usuario usuario = usuarioRepository.findById(miembro.getId_usuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario not found"));
        boolean puedeEjecutar = usuario.getRol().getRolPermisos().stream()
                .anyMatch(rolPermiso -> PERMISO_MIEMBRO.equals(rolPermiso.getPermiso().getCodigo()));
        if (!puedeEjecutar) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario " + usuario.getNombres() + " " + usuario.getApellidos()
                            + " no tiene un rol que ejecute Órdenes de Trabajo.");
        }
        UsuarioEspecialidad pertenencia = new UsuarioEspecialidad();
        pertenencia.setUsuario(usuario);
        pertenencia.setEspecialidad(especialidad);
        pertenencia.setEs_responsable(miembro.getEs_responsable());
        return pertenencia;
    }

    private MiembroEspecialidadResponse toMiembroResponse(UsuarioEspecialidad pertenencia) {
        MiembroEspecialidadResponse response = new MiembroEspecialidadResponse();
        response.setId_usuario(pertenencia.getUsuario().getId_usuario());
        response.setNombres(pertenencia.getUsuario().getNombres());
        response.setApellidos(pertenencia.getUsuario().getApellidos());
        response.setEs_responsable(pertenencia.getEs_responsable());
        return response;
    }

    private MiEspecialidadResponse toMiEspecialidadResponse(UsuarioEspecialidad pertenencia) {
        MiEspecialidadResponse response = new MiEspecialidadResponse();
        response.setId_especialidad(pertenencia.getEspecialidad().getId_especialidad());
        response.setNombre(pertenencia.getEspecialidad().getNombre());
        response.setEs_responsable(pertenencia.getEs_responsable());
        return response;
    }
}
