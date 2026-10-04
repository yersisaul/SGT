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
 * Pertenencia de un usuario a una especialidad (PRD D2, N:M) y si es
 * responsable de ella (D5). Unidireccional: Usuario y Especialidad no
 * exponen la colección inversa (CLAUDE.md 5).
 */
@Entity
@Table(name = "usuario_especialidad",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_usuario", "id_especialidad"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UsuarioEspecialidad {
    @Id
    @Column(name = "id_usuario_especialidad")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id_usuario_especialidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidad especialidad;

    @Column(name = "es_responsable", nullable = false)
    private Boolean es_responsable = Boolean.FALSE;
}
