package aplicacion;

import modelos.Pedido;
import modelos.PedidoComida;
import modelos.PedidoEncomienda;
import modelos.PedidoExpress;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== SISTEMA SPEEDFAST ===");
        System.out.println();

        PedidoComida comida = new PedidoComida(101, "Av. Providencia 123", 8.5, true);
        PedidoEncomienda encomienda = new PedidoEncomienda(202, "Calle Lota 456", 3.0, 12.5, true);
        PedidoExpress express = new PedidoExpress(303, "Av. Las Condes 789", 7.2, 1.8);
        PedidoExpress expressMenor = new PedidoExpress(303, "Av. Las Condes 789", 4.3, 1.8);

        System.out.println("--- Resumen y tiempo de entrega ---");
        Pedido[] pedidos = { comida, encomienda, express, expressMenor };
        for (Pedido pedido : pedidos) {
            pedido.mostrarResumen();
            System.out.println("Tiempo estimado:  " + (int) pedido.calcularTiempoEntrega() + " minutos");
            System.out.println();
        }

        System.out.println("--- Cálculo de Tiempos ---");
        System.out.println("Comida:       (15min + 2 por cada kilómetro), cálculo: 15 + (2 x " + comida.getDistanciaKm() + "), tiempo total: " + (int) comida.calcularTiempoEntrega() + " minutos");
        System.out.println("Encomienda:   (20min + 1.5 min por kilómetro), cálculo: 20 + (1.5 x " + encomienda.getDistanciaKm() + "), tiempo total: " + (int) encomienda.calcularTiempoEntrega() + " minutos");

        System.out.println();
        System.out.println("---               Cálculo envío express                  ---");
        System.out.println("--- 10min base, pero si es > 5km, se agregan 5 min extra ---");
        System.out.println();

        System.out.println("Express >5km: " + express.getDistanciaKm() + "km > 5km, entonces 10 + 5, timepo total: " + (int) express.calcularTiempoEntrega() + " minutos");
        System.out.println("Express <5km: " + expressMenor.getDistanciaKm() + "km < 5km, entonces 10, tiempo total: " + (int) expressMenor.calcularTiempoEntrega() + " minutos");
        System.out.println();

        /* DESARROLLO DE SEMANA 1 - COMENTADO PARA NO SATURAR SALIDA.
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
