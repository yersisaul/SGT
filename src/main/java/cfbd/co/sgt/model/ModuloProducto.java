package cfbd.co.sgt.model;

/**
 * Elemento de la lista JSONB {@code web_producto.modulos}: un módulo del
 * Panel Azor o una analítica de Azor Analytics, agrupados por {@code grupo}
 * (p. ej. "Tráfico y vehículos").
 */
public record ModuloProducto(String nombre, String grupo, String descripcion) {
}
