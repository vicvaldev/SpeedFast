package aplicacion;

import modelos.implementacion.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== SISTEMA SPEEDFAST ===");

        PedidoComida comida = new PedidoComida(101, "Av. Providencia 123", 8.5, true);
        PedidoEncomienda encomienda = new PedidoEncomienda(202, "Calle Lota 456", 3.0, 12.5, true);
        PedidoExpress express = new PedidoExpress(303, "Av. Las Condes 789", 7.2, 1.8);
        PedidoExpress expressMenor = new PedidoExpress(404, "Av. Las Condes 789", 4.3, 1.8);
        PedidoComida comida2 = new PedidoComida(505, "Pasaje Los Alerces 910", 5.0, false);
        PedidoEncomienda encomienda2 = new PedidoEncomienda(606, "Av. Vicuña Mackenna 111", 10.2, 8.9, true);

        /*ControladorDeEnvios controlador = new ControladorDeEnvios();

        System.out.println("--- Reserva de pedidos ---");
        controlador.reservarPedido(comida);
        controlador.reservarPedido(encomienda);
        controlador.reservarPedido(express);
        controlador.reservarPedido(expressMenor);
        controlador.reservarPedido(comida2);
        controlador.reservarPedido(encomienda2);
        System.out.println(); */

        /*System.out.println("--- Resumen y tiempo de entrega ---");*/
        Pedido[] pedidos = { comida, encomienda, express, expressMenor };
        /*for (Pedido pedido : pedidos) {
            pedido.mostrarResumen();
            System.out.println("Tiempo estimado de entrega:  " + (int) pedido.calcularTiempoEntrega() + " minutos");
            System.out.println("Estado:                      " + pedido.getEstado());
            System.out.println();
        } */

        /*System.out.println("--- Asignación de repartidores ---");
        System.out.println("[Asignación manual - Pedido Comida]");
        comida.asignarRepartidor("Juan Pérez");
        System.out.println();
        System.out.println("[Asignación manual - Pedido Encomienda]");
        encomienda.asignarRepartidor("Camila Soto");
        System.out.println();

        System.out.println("--- Asignación automática (polimorfismo) ---");
        for (Pedido pedido : pedidos) {
            System.out.println("[" + pedido.getTipoPedido() + "]");
            pedido.asignarRepartidor();
            System.out.println();
        }

        System.out.println("--- Despacho de pedidos ---");
        controlador.despachar(comida);
        controlador.despachar(encomienda);
        controlador.despachar(express);
        System.out.println();

        System.out.println("--- Cancelación de un pedido ---");
        controlador.cancelar(expressMenor);
        System.out.println("Estado actual del pedido " + expressMenor.getIdPedido() + ": " + expressMenor.getEstado());
        System.out.println();

        controlador.verHistorial();*/

        System.out.println();
        System.out.println("=== SIMULACION CONCURRENTE (HILOS) ===");
        System.out.println("Enviando 3 repartidores en paralelo...");

        Repartidor juan = new Repartidor("Juan Pérez", List.of(comida, comida2));
        Repartidor camila = new Repartidor("Camila Soto", List.of(encomienda, encomienda2));
        Repartidor luis = new Repartidor("Luis Díaz", List.of(express, expressMenor));

        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.execute(juan);
        executor.execute(camila);
        executor.execute(luis);

        executor.shutdown();

        try {
            boolean terminaron = executor.awaitTermination(120, TimeUnit.SECONDS);
            if (terminaron) {
                System.out.println();
                System.out.println("==> Todos los repartidores terminaron sus entregas.");
            }
        } catch (InterruptedException e) {
            System.out.println("Simulación interrumpida.");
            Thread.currentThread().interrupt();
        }

        /* DESARROLLO DE SEMANA 2.
        System.out.println("--- Cálculo de Tiempos ---");
        System.out.println("Comida:       (15min + 2 por cada kilómetro), cálculo: 15 + (2 x " + comida.getDistanciaKm() + "), tiempo total: " + (int) comida.calcularTiempoEntrega() + " minutos");
        System.out.println("Encomienda:   (20min + 1.5 min por kilómetro), cálculo: 20 + (1.5 x " + encomienda.getDistanciaKm() + "), tiempo total: " + (int) encomienda.calcularTiempoEntrega() + " minutos");

        System.out.println();
        System.out.println("---               Cálculo envío express                  ---");
        System.out.println("--- 10min base, pero si es > 5km, se agregan 5 min extra ---");
        System.out.println();

        System.out.println("Express >5km: " + express.getDistanciaKm() + "km > 5km, entonces 10 + 5, tiempo total: " + (int) express.calcularTiempoEntrega() + " minutos");
        System.out.println("Express <5km: " + expressMenor.getDistanciaKm() + "km < 5km, entonces 10, tiempo total: " + (int) expressMenor.calcularTiempoEntrega() + " minutos");
        System.out.println(); */

        /* DESARROLLO DE SEMANA 1.
        System.out.println("--- Demostracion de sobrecarga ---");

        System.out.println("[Pedido Comida]");
        comida.asignarRepartidor("Juan Pérez");
        System.out.println();

        System.out.println("[Pedido Encomienda]");
        encomienda.asignarRepartidor("Camila Soto");
        System.out.println();

        System.out.println("[Pedido Express]");
        express.asignarRepartidor("Luis Díaz");
        System.out.println();

        System.out.println("--- Demostracion de polimorfismo (referencia tipo Pedido) ---");
        for (Pedido pedido : pedidos) {
            System.out.println("[" + pedido.getTipoPedido() + "]");
            pedido.asignarRepartidor();
        }
        */
    }
}
