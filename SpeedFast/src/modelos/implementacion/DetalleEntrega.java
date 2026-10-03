package modelos.implementacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Fila de la tabla de entregas ya resuelta con los datos del pedido y del
 * repartidor.
 *
 * <p>La tabla {@code entrega} solo guarda las dos claves foraneas y el
 * instante, asi que mostrar "Camino 42" o "Camila Rojas" en la interfaz exige
 * unir con {@code pedido} y {@code repartidor}. Ese JOIN se resuelve una sola
 * vez en el DAO y viaja aqui como un objeto inmutable, de modo que la vista
 * solo tenga que pintar filas sin volver a consultar la base de datos.</p>
 */
public class DetalleEntrega {

    private final int idEntrega;
    private final int idPedido;
    private final String tipoPedido;
    private final String direccionEntrega;
    private final int idRepartidor;
    private final String nombreRepartidor;
    private final LocalDate fecha;
    private final LocalTime hora;

    public DetalleEntrega(int idEntrega, int idPedido, String tipoPedido, String direccionEntrega,
                          int idRepartidor, String nombreRepartidor, LocalDate fecha, LocalTime hora) {
        this.idEntrega = idEntrega;
        this.idPedido = idPedido;
        this.tipoPedido = tipoPedido;
        this.direccionEntrega = direccionEntrega;
        this.idRepartidor = idRepartidor;
        this.nombreRepartidor = nombreRepartidor;
        this.fecha = fecha;
        this.hora = hora;
    }

    public int getIdEntrega() {
        return idEntrega;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public String getTipoPedido() {
        return tipoPedido;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public int getIdRepartidor() {
        return idRepartidor;
    }

    public String getNombreRepartidor() {
        return nombreRepartidor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public LocalDateTime getFechaHora() {
        return LocalDateTime.of(fecha, hora);
    }

    /**
     * Reconstruye el {@link Entrega} que corresponde a esta fila, para poder
     * editarla o eliminarla desde la tabla.
     */
    public Entrega toEntrega() {
        return new Entrega(idEntrega, idPedido, idRepartidor, fecha, hora);
    }
}
