package cfbd.co.sgt.service.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cfbd.co.sgt.dto.request.ConfiguracionSlaRequest;
import cfbd.co.sgt.dto.response.ConfiguracionSlaResponse;
import cfbd.co.sgt.model.ClaveSla;
import cfbd.co.sgt.model.ConfiguracionSla;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.ConfiguracionSlaRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.ConfiguracionSlaService;

@Service
public class ConfiguracionSlaServiceImpl implements ConfiguracionSlaService {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionSlaServiceImpl.class);

    private final ConfiguracionSlaRepository repository;
    private final UsuarioActualProvider usuarioActual;
    /** Valores iniciales del .env; también se usan si falta una fila. */
    private final Map<ClaveSla, Integer> porDefecto = new EnumMap<>(ClaveSla.class);
    private volatile Map<ClaveSla, Long> cache;

    public ConfiguracionSlaServiceImpl(ConfiguracionSlaRepository repository, UsuarioActualProvider usuarioActual,
            @Value("${app.sla.despacho.alta-horas:1}") int despachoAlta,
            @Value("${app.sla.despacho.media-horas:2}") int despachoMedia,
            @Value("${app.sla.despacho.baja-horas:4}") int despachoBaja,
            @Value("${app.sla.despacho.requerimiento-horas:2}") int despachoRequerimiento,
            @Value("${app.sla.atencion.alta-horas:8}") int atencionAlta,
            @Value("${app.sla.atencion.media-horas:24}") int atencionMedia,
            @Value("${app.sla.atencion.baja-horas:72}") int atencionBaja) {
        this.repository = repository;
        this.usuarioActual = usuarioActual;
        porDefecto.put(ClaveSla.DESPACHO_ALTA, despachoAlta);
        porDefecto.put(ClaveSla.DESPACHO_MEDIA, despachoMedia);
        porDefecto.put(ClaveSla.DESPACHO_BAJA, despachoBaja);
        porDefecto.put(ClaveSla.DESPACHO_REQUERIMIENTO, despachoRequerimiento);
        porDefecto.put(ClaveSla.ATENCION_ALTA, atencionAlta);
        porDefecto.put(ClaveSla.ATENCION_MEDIA, atencionMedia);
        porDefecto.put(ClaveSla.ATENCION_BAJA, atencionBaja);
    }

    @Override
    @Transactional
    public void inicializar() {
        for (ClaveSla clave : ClaveSla.values()) {
            if (repository.findByClave(clave).isEmpty()) {
                ConfiguracionSla nueva = new ConfiguracionSla();
                nueva.setClave(clave);
                nueva.setHoras(porDefecto.get(clave));
                repository.save(nueva);
                log.info("DataSeeder: creando SLA '{}' = {} h.", clave, nueva.getHoras());
            }
        }
        invalidarTrasCommit();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionSlaResponse> listar() {
        Map<ClaveSla, ConfiguracionSla> filas = new EnumMap<>(ClaveSla.class);
        repository.findAll().forEach(fila -> filas.put(fila.getClave(), fila));
        return Arrays.stream(ClaveSla.values()).map(clave -> toResponse(clave, filas.get(clave))).toList();
    }

    @Override
    @Transactional
    public List<ConfiguracionSlaResponse> actualizar(ConfiguracionSlaRequest request) {
        Usuario actor = usuarioActual.obtener();
        Instant ahora = Instant.now();
        for (ConfiguracionSlaRequest.Valor valor : request.getValores()) {
            ConfiguracionSla fila = repository.findByClave(valor.getClave()).orElseGet(() -> {
                ConfiguracionSla nueva = new ConfiguracionSla();
                nueva.setClave(valor.getClave());
                return nueva;
            });
            if (valor.getHoras().equals(fila.getHoras())) {
                continue;
            }
            log.info("SLA '{}' cambia de {} h a {} h.", valor.getClave(), fila.getHoras(), valor.getHoras());
            fila.setHoras(valor.getHoras());
            fila.setFecha_modificacion(ahora);
            fila.setUsuario_modificacion(actor);
            repository.save(fila);
        }
        invalidarTrasCommit();
        return listar();
    }

    @Override
    public long horas(ClaveSla clave) {
        Map<ClaveSla, Long> vigente = cache;
        if (vigente == null) {
            vigente = cargar();
            cache = vigente;
        }
        return vigente.get(clave);
    }

    private Map<ClaveSla, Long> cargar() {
        Map<ClaveSla, Long> valores = new EnumMap<>(ClaveSla.class);
        porDefecto.forEach((clave, horas) -> valores.put(clave, horas.longValue()));
        repository.findAll().forEach(fila -> valores.put(fila.getClave(), fila.getHoras().longValue()));
        return valores;
    }

    /** La caché se descarta solo tras el commit, para que nadie recargue valores aún no confirmados. */
    private void invalidarTrasCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cache = null;
                }
            });
        } else {
            cache = null;
        }
    }

    private ConfiguracionSlaResponse toResponse(ClaveSla clave, ConfiguracionSla fila) {
        int defecto = porDefecto.get(clave);
        if (fila == null) {
            return new ConfiguracionSlaResponse(clave.name(), clave.grupo(), clave.etiqueta(), defecto, defecto, null, null);
        }
        Usuario usuario = fila.getUsuario_modificacion();
        String modificadoPor = usuario == null ? null : (usuario.getNombres() + " " + usuario.getApellidos()).trim();
        return new ConfiguracionSlaResponse(clave.name(), clave.grupo(), clave.etiqueta(), fila.getHoras(), defecto,
                fila.getFecha_modificacion(), modificadoPor);
    }
}
