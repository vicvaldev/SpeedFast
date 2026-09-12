package modelos.implementacion;

import modelos.contratos.Cancelable;
import modelos.contratos.Despachable;
import modelos.contratos.Rastreable;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ControladorDeEnvios implements Despachable, Cancelable, Rastreable {
    private ArrayList<Pedido> historial;
    private final ReentrantLock bloqueoHistorial = new ReentrantLock();
    private final AtomicInteger entregasRealizadas = new AtomicInteger();

    public ControladorDeEnvios() {
        this.historial = new ArrayList<>();
    }

    // El historial es un recurso compartido entre los hilos repartidores;
    // ReentrantLock garantiza que solo un hilo lo modifica (o lo lee) a la vez.
    private void registrarEnHistorial(Pedido pedido) {
        bloqueoHistorial.lock();
        try {
            historial.add(pedido);
        } finally {
            bloqueoHistorial.unlock();
        }
    }

    // Registra la entrega de forma segura y devuelve el número de entrega.
    public int registrarEntrega(Pedido pedido) {
        int nroEntrega = entregasRealizadas.incrementAndGet();
        registrarEnHistorial(pedido);
        return nroEntrega;
    }

    public int getEntregasRealizadas() {
        return entregasRealizadas.get();
    }

    public int getHistorialSize() {
        bloqueoHistorial.lock();
        try {
            return historial.size();
        } finally {
            bloqueoHistorial.unlock();
        }
    }

    public void reservarPedido(Pedido pedido) {
        pedido.setEstado(EstadoPedido.PENDIENTE);
        System.out.println("→ Pedido " + pedido.getIdPedido() + " (" + pedido.getTipoPedido() + ") reservado.");
    }

    @Override
    public void despachar(Pedido pedido) {
        System.out.println("Despachando pedido " + pedido.getIdPedido() + "...");
        pedido.setEstado(EstadoPedido.EN_REPARTO);
        System.out.println();
        System.out.println("→ El pedido " + pedido.getIdPedido() + " (" + pedido.getTipoPedido() + ") ha sido despachado.");
        registrarEnHistorial(pedido);
    }

    @Override
    public void cancelar(Pedido pedido) {
        System.out.println("Cancelando envío...");
        pedido.setEstado(EstadoPedido.PENDIENTE);
        System.out.println("→ El envío del pedido " + pedido.getIdPedido() + " (" + pedido.getTipoPedido() + ") ha sido cancelado.");
    }

    @Override
    public void verHistorial(String nombreRepartidor) {
        System.out.println("=== Historial de entregas realizadas por " + nombreRepartidor + " ===");
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
