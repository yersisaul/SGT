package cfbd.co.sgt.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

/**
 * Columnas de auditoría comunes a las tablas web_* del sitio web público
 * (no crea tabla propia). Las fechas se fijan solas al insertar/actualizar;
 * el usuario lo asigna cada service con el usuario autenticado.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AuditoriaWeb {

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fecha_creacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fecha_actualizacion;

    // Último usuario del panel que creó o modificó el registro.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_actualiza")
    private Usuario usuario_actualiza;

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        fecha_creacion = ahora;
        fecha_actualizacion = ahora;
    }

    @PreUpdate
    void alActualizar() {
        fecha_actualizacion = Instant.now();
    }
}
