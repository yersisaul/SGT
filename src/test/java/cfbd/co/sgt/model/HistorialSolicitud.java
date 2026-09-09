package cfbd.co.sgt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Entity
@Table(name = "historial_solicitud")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class HistorialSolicitud {
    @Id
    @Column(name = "id_historial_solicitud")
    String id_historial_solicitud;

    // Relación con la entidad Solicitud (un historial de solicitud pertenece a una solicitud)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private Solicitud solicitud;

    // Relación con la entidad Usuario (un historial de solicitud pertenece a un usuario)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // Relación con la entidad Estado (un historial de solicitud tiene un estado anterior)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_anterior", nullable = false)
    private Estado estado_anterior;

    // Relación con la entidad Estado (un historial de solicitud tiene un estado nuevo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_nuevo", nullable = false)
    private Estado estado_nuevo;
    
    @Column(name = "fecha")
    String fecha;

    @Column (name = "comentario")
    String comentario;

}
