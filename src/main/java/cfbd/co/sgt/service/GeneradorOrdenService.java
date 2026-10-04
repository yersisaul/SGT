package cfbd.co.sgt.service;

import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.model.Usuario;

/**
 * Crea una OT en la cola de una especialidad:
 * estado "Pendiente", sin ejecutor, con su historial de creación y el evento
 * ENCOLADA. Exactamente uno de solicitud/requerimiento es el origen.
 */
public interface GeneradorOrdenService {

    Orden generarEnCola(Especialidad especialidad, Solicitud solicitud, Requerimiento requerimiento,
                        Usuario actor, String comentario);
}
