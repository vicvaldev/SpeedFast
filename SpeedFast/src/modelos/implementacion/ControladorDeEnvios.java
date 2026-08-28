package modelos.implementacion;

import modelos.contratos.Cancelable;
import modelos.contratos.Despachable;
import modelos.contratos.Rastreable;

import java.util.ArrayList;

public class ControladorDeEnvios implements Despachable, Cancelable, Rastreable {
    private ArrayList<Pedido> historial;

    public ControladorDeEnvios() {
        this.historial = new ArrayList<>();
    }

    public void reservarPedido(Pedido pedido) {
        pedido.setEstado("Reservado");
        System.out.println("→ Pedido " + pedido.getIdPedido() + " (" + pedido.getTipoPedido() + ") reservado.");
    }

    @Override
    public void despachar(Pedido pedido) {
        System.out.println("Despachando pedido " + pedido.getIdPedido() + "...");
        pedido.setEstado("Despachado");
        System.out.println("→ El pedido " + pedido.getIdPedido() + " (" + pedido.getTipoPedido() + ") ha sido despachado.");
        historial.add(pedido);
    }

    @Override
    public void cancelar(Pedido pedido) {
        System.out.println("Cancelando envío...");
        pedido.setEstado("Cancelado");
        System.out.println("→ El envío del pedido " + pedido.getIdPedido() + " (" + pedido.getTipoPedido() + ") ha sido cancelado.");
    }

    @Override
    public void verHistorial() {
        System.out.println("=== Historial de entregas realizadas ===");
        if (historial.isEmpty()) {
            System.out.println("→ No hay entregas registradas aún.");
            return;
        }
        for (int i = 0; i < historial.size(); i++) {
            Pedido pedido = historial.get(i);
            System.out.println((i + 1) + ". Pedido " + pedido.getIdPedido()
                    + " | Tipo: " + pedido.getTipoPedido()
                    + " | Tiempo: " + (int) pedido.calcularTiempoEntrega() + " min"
                    + " | Estado: " + pedido.getEstado());
        }
        System.out.println("→ Total de entregas registradas: " + historial.size());
    }
}
