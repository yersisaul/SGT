package cfbd.co.sgt.model;

import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Especialidades en las que un activo puede generar Solicitudes (decisión
 * 2026-10-04: un activo puede pertenecer a varias). La principal sigue
 * siendo activo.id_especialidad, con la que nace la Solicitud; esta tabla
 * la incluye junto con las demás. Unidireccional (CLAUDE.md 5).
 */
@Entity
@Table(name = "activo_especialidad",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_activo", "id_especialidad"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ActivoEspecialidad {
    @Id
    @Column(name = "id_activo_especialidad")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id_activo_especialidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_activo", nullable = false)
    private Activo activo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidad especialidad;
}
