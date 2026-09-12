package modelos.implementacion;

public class Repartidor implements Runnable {
    private static final long FACTOR_PAUSA_MS = 150L;

    private final String nombre;
    private final ZonaDeCarga zonaDeCarga;
    private final ControladorDeEnvios controladorDeEnvios;

    public Repartidor(String nombre, ZonaDeCarga zonaDeCarga, ControladorDeEnvios controladorDeEnvios) {
        this.nombre = nombre;
        this.zonaDeCarga = zonaDeCarga;
        this.controladorDeEnvios = controladorDeEnvios;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public void run() {
        while (true) {
            Pedido pedido;
            try {
                // Consumidor: espera con wait() si la zona está vacía y devuelve
                // null solo cuando la producción cerró y la zona quedó vacía.
                pedido = zonaDeCarga.retirarPedido();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Repartidor - " + nombre + "] Interrupción recibida. Finalizando.");
                return;
            }
            if (pedido == null) {
                break;
            }

            // Solo el hilo que obtuvo el pedido modifica su estado.
            pedido.setEstado(EstadoPedido.EN_REPARTO);
            System.out.println("[Repartidor - " + nombre + "] Retirando pedido #" + pedido.getIdPedido() + "...");
            System.out.println("[Repartidor - " + nombre + "] Estado: " + pedido.getEstado());
            System.out.println("[Repartidor - " + nombre + "] Entregando pedido #" + pedido.getIdPedido() + "...");

            try {
                // Simulacion del tiempo de entrega (fuera de la sección crítica).
                long pausa = Math.round(pedido.calcularTiempoEntrega()) * FACTOR_PAUSA_MS
                        + (pedido.getIdPedido() % 5) * 130L;
                Thread.sleep(pausa);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Repartidor - " + nombre + "] Entrega interrumpida.");
                return;
            }

            pedido.setEstado(EstadoPedido.ENTREGADO);
            int nroEntrega = controladorDeEnvios.registrarEntrega(pedido);
            System.out.println("[Repartidor - " + nombre + "] Estado: " + pedido.getEstado()
                    + " (pedido #" + pedido.getIdPedido() + " entregado). Entrega registrada No. " + nroEntrega);
        }

        System.out.println("[Repartidor - " + nombre + "] No hay mas pedidos. Finalizando turno.");
    }
}
