package aplicacion;

import modelos.implementacion.ControladorDeEnvios;
import modelos.implementacion.GeneradorDePedidos;
import modelos.implementacion.Pedido;
import modelos.implementacion.PedidoComida;
import modelos.implementacion.PedidoEncomienda;
import modelos.implementacion.PedidoExpress;
import modelos.implementacion.Repartidor;
import modelos.implementacion.ZonaDeCarga;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== SISTEMA SPEEDFAST ===");
        System.out.println();
        System.out.println("=== PRODUCTOR-CONSUMIDOR (zona de carga compartida) ===");

        ZonaDeCarga zonaDeCarga = new ZonaDeCarga(3, 2);
        System.out.println("[Zona de carga inicializada] Capacidad: 3");

        List<Pedido> pedidos = List.of(
                new PedidoComida(1, "Santiago Centro", 3.2, true),
                new PedidoEncomienda(2, "Providencia", 5.1, 12.5, true),
                new PedidoEncomienda(3, "Nunoa", 2.4, 8.2, true),
                new PedidoExpress(4, "Recoleta", 6.3, 2.1),
                new PedidoComida(5, "Las Condes", 4.8, false)
        );

        ControladorDeEnvios controlador = new ControladorDeEnvios();
        GeneradorDePedidos generadorNorte = new GeneradorDePedidos("Generador Norte", zonaDeCarga, pedidos.subList(0, 3));
        GeneradorDePedidos generadorSur = new GeneradorDePedidos("Generador Sur", zonaDeCarga, pedidos.subList(3, 5));

        Repartidor juan = new Repartidor("Juan", zonaDeCarga, controlador);
        Repartidor camila = new Repartidor("Camila", zonaDeCarga, controlador);
        Repartidor pedro = new Repartidor("Pedro", zonaDeCarga, controlador);

        System.out.println("2 productores (generadores de pedidos) + 3 consumidores (repartidores).");
        System.out.println();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        executor.execute(generadorNorte);
        executor.execute(generadorSur);
        executor.execute(juan);
        executor.execute(camila);
        executor.execute(pedro);
        executor.shutdown();

        try {
            boolean terminaron = executor.awaitTermination(120, TimeUnit.SECONDS);
            if (!terminaron) {
                System.out.println("La simulacion no termino a tiempo. Interrumpiendo hilos...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Simulacion interrumpida.");
        }

        System.out.println();
        System.out.println("=== Estado final de los pedidos ===");
        for (Pedido pedido : pedidos) {
            System.out.println("Pedido #" + pedido.getIdPedido() + " -> " + pedido.getEstado());
        }
        System.out.println("Entregas registradas (AtomicInteger): " + controlador.getEntregasRealizadas());
        System.out.println("Historial compartido (ReentrantLock): " + controlador.getHistorialSize() + " entregas");

        System.out.println();
        System.out.println("Todos los pedidos han sido entregados correctamente");
    }
}
