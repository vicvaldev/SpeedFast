package modelos.implementacion;

import java.util.List;

public class GeneradorDePedidos implements Runnable {
    private static final long PAUSA_PREPARACION_MS = 700L;

    private final String nombre;
    private final ZonaDeCarga zonaDeCarga;
    private final List<Pedido> pedidosPorGenerar;

    public GeneradorDePedidos(ZonaDeCarga zonaDeCarga, List<Pedido> pedidosPorGenerar) {
        this("Generador", zonaDeCarga, pedidosPorGenerar);
    }

    public GeneradorDePedidos(String nombre, ZonaDeCarga zonaDeCarga, List<Pedido> pedidosPorGenerar) {
        this.nombre = nombre;
        this.zonaDeCarga = zonaDeCarga;
        this.pedidosPorGenerar = pedidosPorGenerar;
    }

    @Override
    public void run() {
        try {
            for (Pedido pedido : pedidosPorGenerar) {
                Thread.sleep(PAUSA_PREPARACION_MS);
                System.out.println("[" + nombre + "] Pedido #" + pedido.getIdPedido()
                        + " preparado (" + pedido.getTipoPedido() + "). Cargando a la zona...");
                // Bloquea con wait() si la zona está llena.
                zonaDeCarga.agregarPedido(pedido);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[" + nombre + "] Produccion interrumpida.");
        } finally {
            // Imprescindible: sin esto los repartidores esperarian para siempre.
            zonaDeCarga.cerrarProduccion();
            System.out.println("[" + nombre + "] Produccion finalizada.");
        }
    }
}
