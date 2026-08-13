package aplicacion;

import modelos.Pedido;
import modelos.PedidoComida;
import modelos.PedidoEncomienda;
import modelos.PedidoExpress;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== SISTEMA SPEEDFAST - ASIGNACION DE REPARTIDORES ===");
        System.out.println();

        PedidoComida comida = new PedidoComida(101, "Av. Providencia 123", true);
        PedidoEncomienda encomienda = new PedidoEncomienda(202, "Calle Lota 456", 12.5, true);
        PedidoExpress express = new PedidoExpress(303, "Av. Las Condes 789", 1.8);

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
        Pedido[] pedidos = { comida, encomienda, express };
        for (Pedido pedido : pedidos) {
            System.out.println("[" + pedido.getTipoPedido() + "]");
            pedido.asignarRepartidor();
        }
    }
}
