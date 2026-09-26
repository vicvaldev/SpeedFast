package modelos.implementacion;

import java.time.LocalDateTime;

/**
 * Entrada inmutable del historial de entregas.
 *
 * <p>El historial solo podia guardar el {@link Pedido}, lo que hacia perder el
 * nombre del repartidor que realizo la entrega. Este DTO captura los datos ya
 * resueltos en el momento de la entrega para que la vista del historial no
 * necesite volver a consultar la base de datos ni depender del estado actual
 * del pedido.</p>
 */
public class RegistroEntrega {

    private final int idPedido;
    private final String tipoPedido;
    private final String direccionEntrega;
    private final String nombreRepartidor;
    private final int tiempoEntregaMin;
    private final EstadoPedido estado;
    private final LocalDateTime fechaHora;

    public RegistroEntrega(Pedido pedido, String nombreRepartidor, LocalDateTime fechaHora) {
        this.idPedido = pedido.getIdPedido();
        this.tipoPedido = pedido.getTipoPedido();
        this.direccionEntrega = pedido.getDireccionEntrega();
        this.nombreRepartidor = nombreRepartidor;
        this.tiempoEntregaMin = (int) pedido.calcularTiempoEntrega();
        this.estado = pedido.getEstado();
        this.fechaHora = fechaHora;
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

    public String getNombreRepartidor() {
        return nombreRepartidor;
    }

    public int getTiempoEntregaMin() {
        return tiempoEntregaMin;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
