package cfbd.co.sgt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Entity
@Table(name = "estado")
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class Estado {
    @Id
    @Column(name = "id_estado")
    String id_estado;

    @Column(name = "nombre")
    String nombre;

    // Relación con la entidad Requerimiento (un estado puede tener muchos requerimientos)
    @OneToMany(mappedBy = "id_estado", fetch = FetchType.LAZY)
    private List<Requerimiento> requerimientos = new ArrayList<>();

    // Relación con la entidad HistorialOrden (un estado puede ser el estado anterior de muchos historiales de orden)
    @OneToMany(mappedBy = "estado_anterior", fetch = FetchType.LAZY)
    private List<HistorialOrden> historiales_anteriores_ordenes = new ArrayList<>();

    // Relación con la entidad HistorialOrden (un estado puede ser el estado nuevo de muchos historiales de orden)
    @OneToMany(mappedBy = "estado_nuevo", fetch = FetchType.LAZY)
    private List<HistorialOrden> historiales_nuevos_ordenes = new ArrayList<>();

    // Relación con la entidad HistorialRequerimiento (un estado puede ser el estado anterior de muchos historiales de requerimiento)
    @OneToMany(mappedBy = "estado_anterior", fetch = FetchType.LAZY)
    private List<HistorialRequerimiento> historiales_anteriores_requerimientos = new ArrayList<>();

    // Relación con la entidad HistorialRequerimiento (un estado puede ser el estado nuevo de muchos historiales de requerimiento)
    @OneToMany(mappedBy = "estado_nuevo", fetch = FetchType.LAZY)
    private List<HistorialRequerimiento> historiales_nuevos_requerimientos = new ArrayList<>();

    // Relación con la entidad HistorialSolicitud (un estado puede ser el estado anterior de muchos historiales de solicitud)
    @OneToMany(mappedBy = "estado_anterior", fetch = FetchType.LAZY)
    private List<HistorialSolicitud> historiales_anteriores_solicitudes = new ArrayList<>();

    // Relación con la entidad HistorialSolicitud (un estado puede ser el estado nuevo de muchos historiales de solicitud)
    @OneToMany(mappedBy = "estado_nuevo", fetch = FetchType.LAZY)
    private List<HistorialSolicitud> historiales_nuevos_solicitudes = new ArrayList<>();

    // Relación con la entidad Orden (un estado puede tener muchas órdenes)
    @OneToMany(mappedBy = "estado", fetch = FetchType.LAZY)
    private List<Orden> ordenes = new ArrayList<>();

    // Relación con la entidad Solicitud (un estado puede tener muchas solicitudes)
    @OneToMany(mappedBy = "estado", fetch = FetchType.LAZY)
    private List<Solicitud> solicitudes = new ArrayList<>();
}
