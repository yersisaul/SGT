package cfbd.co.sgt.service.impl;

import org.springframework.stereotype.Service;

import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.repository.UsuarioEspecialidadRepository;
import cfbd.co.sgt.security.Permisos;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.AutorizacionRecursoService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutorizacionRecursoServiceImpl implements AutorizacionRecursoService {

    private final UsuarioActualProvider usuarioActual;
    private final SolicitudRepository solicitudRepository;
    private final UsuarioEspecialidadRepository usuarioEspecialidadRepository;

    @Override
    public boolean veTodasLasSolicitudes() {
        return usuarioActual.tienePermiso(Permisos.SOLICITUD_READ_ALL);
    }

    @Override
    public boolean veTodosLosRequerimientos() {
        return usuarioActual.tienePermiso(Permisos.REQUERIMIENTO_READ_ALL);
    }

    @Override
    public boolean veTodasLasOrdenes() {
        return usuarioActual.tienePermiso(Permisos.ORDEN_READ_ALL);
    }

    @Override
    public boolean puedeVer(Usuario actor, Solicitud solicitud) {
        return veTodasLasSolicitudes()
                || solicitudRepository.esVisiblePara(solicitud.getId_solicitud(), actor.getId_usuario());
    }

    @Override
    public boolean puedeVer(Usuario actor, Requerimiento requerimiento) {
        if (veTodosLosRequerimientos() || esMismoUsuario(requerimiento.getUsuario(), actor)) {
            return true;
        }
        Solicitud origen = requerimiento.getSolicitud();
        if (origen != null && esMismoUsuario(origen.getUsuario(), actor)) {
            return true;
        }
        return requerimiento.getOrdenes().stream()
                .anyMatch(orden -> esMismoUsuario(orden.getUsuario(), actor) || esMiembro(actor, orden.getEspecialidad()));
    }

    @Override
    public boolean puedeVer(Usuario actor, Orden orden) {
        if (veTodasLasOrdenes() || esMismoUsuario(orden.getUsuario(), actor)
                || esMiembro(actor, orden.getEspecialidad())) {
            return true;
        }
        Solicitud origen = orden.getSolicitud() != null
                ? orden.getSolicitud()
                : (orden.getRequerimiento() != null ? orden.getRequerimiento().getSolicitud() : null);
        return origen != null && esMismoUsuario(origen.getUsuario(), actor);
    }

    @Override
    public boolean esMiembro(Usuario actor, Especialidad especialidad) {
        return usuarioEspecialidadRepository.esMiembro(actor.getId_usuario(), especialidad.getId_especialidad());
    }

    @Override
    public boolean esResponsable(Usuario actor, Especialidad especialidad) {
        return usuarioEspecialidadRepository.esResponsable(actor.getId_usuario(), especialidad.getId_especialidad());
    }

    @Override
    public void exigirVisible(Usuario actor, Solicitud solicitud) {
        if (!puedeVer(actor, solicitud)) {
            throw new ResourceNotFoundException("Solicitud not found");
        }
    }

    @Override
    public void exigirVisible(Usuario actor, Requerimiento requerimiento) {
        if (!puedeVer(actor, requerimiento)) {
            throw new ResourceNotFoundException("Requerimiento not found");
        }
    }

    @Override
    public void exigirVisible(Usuario actor, Orden orden) {
        if (!puedeVer(actor, orden)) {
            throw new ResourceNotFoundException("Orden not found");
        }
    }

    private boolean esMismoUsuario(Usuario usuario, Usuario actor) {
        return usuario != null && usuario.getId_usuario().equals(actor.getId_usuario());
    }
}
