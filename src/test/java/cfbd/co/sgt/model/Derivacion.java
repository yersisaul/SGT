package cfbd.co.sgt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Entity
@Table(name = "derivacion")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class Derivacion {
    @Id
    @Column(name = "id_derivacion")
    String id_derivacion;

    // Relación con la entidad Solicitud (una derivación pertenece a una solicitud)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private Solicitud solicitud;

    // Relación con la entidad Usuario (una derivación pertenece a un usuario)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_origen", nullable = false)
    private Usuario usuario_origen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_destino", nullable = false)
    private Usuario usuario_destino;

    @Column(name = "fecha_derivacion")
    String fecha_derivacion;

    @Column (name = "motivo")
    String motivo;

    @Column (name = "observacion") 
    String observacion;

}
