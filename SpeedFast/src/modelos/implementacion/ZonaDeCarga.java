package modelos.implementacion;

import java.util.LinkedList;
import java.util.Queue;

public class ZonaDeCarga {
    private static final int CAPACIDAD_DEFAULT = 10;

    private final Queue<Pedido> pedidos = new LinkedList<>();
    private final int capacidad;
    private int productoresActivos;
    private boolean produccionCerrada;

    public ZonaDeCarga() {
        this(CAPACIDAD_DEFAULT, 1);
    }

    public ZonaDeCarga(int capacidad) {
        this(capacidad, 1);
    }

    public ZonaDeCarga(int capacidad, int productoresActivos) {
        this.capacidad = capacidad;
        this.productoresActivos = productoresActivos;
    }

    // Sección crítica - productor: wait() mientras la zona esté llena, soltando el
    // monitor. Al ser synchronized, solo un hilo modifica la zona a la vez y todo
    // hilo que espera reanuda cuando un consumidor retira un pedido y avisa.
    public synchronized void agregarPedido(Pedido p) throws InterruptedException {
        while (pedidos.size() == capacidad) {
            System.out.println("[Zona de carga] Zona llena. El productor espera que se retire un pedido...");
            wait();
        }
        pedidos.add(p);
        System.out.println("[Zona de carga] Pedido #" + p.getIdPedido()
                + " agregado. Destino: " + p.getDireccionEntrega()
                + " (en zona de carga: " + pedidos.size() + " de " + capacidad + ")");
        notifyAll();
    }

    // Sección crítica - consumidor: wait() mientras la zona esté vacía y la
    // producción no haya terminado. El pedido se elimina dentro del bloque
    // synchronized, por lo que cada uno es retirado por un único repartidor.
    public synchronized Pedido retirarPedido() throws InterruptedException {
        while (pedidos.isEmpty() && !produccionCerrada) {
            System.out.println("[Zona de carga] Sin pedidos disponibles. Un repartidor espera...");
            wait();
        }
        Pedido pedido = pedidos.poll();
        if (pedido != null) {
            notifyAll();
        }
        return pedido;
    }

    // Cada productor avisa al terminar. La zona solo cierra cuando no quedan
    // productores activos, evitando que los consumidores finalicen antes de tiempo.
    public synchronized void cerrarProduccion() {
        if (productoresActivos > 0) {
            productoresActivos--;
        }
        if (productoresActivos == 0) {
            produccionCerrada = true;
            notifyAll();
        }
    }
}
