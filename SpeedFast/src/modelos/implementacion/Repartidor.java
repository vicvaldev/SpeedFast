package modelos.implementacion;

import java.util.List;
import java.util.Random;

public class Repartidor implements Runnable {
    private String nombre;
    private List<Pedido> pedidosAsignados;
    private Random random;
    private ControladorDeEnvios controladorDeEnvios = new ControladorDeEnvios();

    public Repartidor(String nombre, List<Pedido> pedidosAsignados) {
        this.nombre = nombre;
        this.pedidosAsignados = pedidosAsignados;
        this.random = new Random();
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public void run() {
        System.out.println("=> " + nombre + " comienza su ruta con " + pedidosAsignados.size() + " pedido(s).");

        for (Pedido pedido : pedidosAsignados) {
            pedido.setEstado("En ruta");
            System.out.println("Repartidor en ruta: [" + nombre + "]");
            try {
                long pausa = Math.round(pedido.calcularTiempoEntrega()) * 200L + random.nextInt(500);
                Thread.sleep(pausa);
            } catch (InterruptedException e) {
                System.out.println("[" + nombre + "] Entrega interrumpida.");
                Thread.currentThread().interrupt();
                return;
            }
            controladorDeEnvios.despachar(pedido);
            pedido.setEstado("Entregado");
            controladorDeEnvios.verHistorial(nombre);
        }

        System.out.println("==> " + nombre + " terminó todos sus pedidos.");
    }
}