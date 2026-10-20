package cfbd.co.sgt.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Horas de un SLA (despacho o atención) editables por el Administrador (aprobado 2026-10-19). */
@Entity
@Table(name = "configuracion_sla")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionSla {

    @Id
    @Column(name = "id_configuracion_sla")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id_configuracion_sla;

    @Enumerated(EnumType.STRING)
    @Column(name = "clave", nullable = false, unique = true, length = 40)
    private ClaveSla clave;

    @Column(name = "horas", nullable = false)
    private Integer horas;

    @Column(name = "fecha_modificacion")
    private Instant fecha_modificacion;

    // Último Administrador que cambió el valor (null mientras conserve el valor inicial del .env).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_modificacion")
    private Usuario usuario_modificacion;
}
