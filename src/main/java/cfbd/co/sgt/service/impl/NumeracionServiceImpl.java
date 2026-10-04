package cfbd.co.sgt.service.impl;

import org.springframework.stereotype.Service;

import cfbd.co.sgt.service.NumeracionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Las secuencias se crean en src/main/resources/db/init.sql (Hibernate con
 * ddl-auto=update no las crea por sí solo).
 */
@Service
public class NumeracionServiceImpl implements NumeracionService {

    private static final String PREFIJO_SOLICITUD = "ST-";
    private static final String PREFIJO_REQUERIMIENTO = "RQ-";
    private static final String PREFIJO_ORDEN = "OT-";
    private static final String SECUENCIA_SOLICITUD = "seq_solicitud";
    private static final String SECUENCIA_REQUERIMIENTO = "seq_requerimiento";
    private static final String SECUENCIA_ORDEN = "seq_orden";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public String siguienteNumeroSolicitud() {
        return PREFIJO_SOLICITUD + siguienteValor(SECUENCIA_SOLICITUD);
    }

    @Override
    public String siguienteNumeroRequerimiento() {
        return PREFIJO_REQUERIMIENTO + siguienteValor(SECUENCIA_REQUERIMIENTO);
    }

    @Override
    public String siguienteNumeroOrden() {
        return PREFIJO_ORDEN + siguienteValor(SECUENCIA_ORDEN);
    }

    // El nombre de la secuencia es siempre una de las constantes de esta
    // clase (nunca entrada del usuario), por eso se puede interpolar.
    private long siguienteValor(String secuencia) {
        Number valor = (Number) entityManager
                .createNativeQuery("select nextval('" + secuencia + "')")
                .getSingleResult();
        return valor.longValue();
    }
}
