package modelos.implementacion;

import modelos.contratos.Cancelable;
import modelos.contratos.Despachable;
import modelos.contratos.Rastreable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ControladorDeEnvios implements Despachable, Cancelable, Rastreable {

    // Etiqueta para los despachos heredados de la app de consola, donde el
    // pedido se registra en el historial sin un repartidor asignado.
    private static final String REPARTIDOR_NO_ASIGNADO = "sin asignar";

    private final List<RegistroEntrega> historial = new ArrayList<>();
    private final ReentrantLock bloqueoHistorial = new ReentrantLock();
    private final AtomicInteger entregasRealizadas = new AtomicInteger();

    public ControladorDeEnvios() {
    }

    // El historial es un recurso compartido entre los hilos repartidores;
    // ReentrantLock garantiza que solo un hilo lo modifica (o lo lee) a la vez.
    private void registrarEnHistorial(RegistroEntrega registro) {
        bloqueoHistorial.lock();
        try {
            historial.add(registro);
        } finally {
            bloqueoHistorial.unlock();
        }
    }

    /**
     * Registra la entrega de forma segura y devuelve el numero de entrega.
     *
     * @param pedido           pedido entregado
     * @param nombreRepartidor repartidor que realizo la entrega
     * @return numero correlativo de entrega
     */
    public int registrarEntrega(Pedido pedido, String nombreRepartidor) {
        int nroEntrega = entregasRealizadas.incrementAndGet();
        registrarEnHistorial(new RegistroEntrega(pedido, nombreRepartidor, LocalDateTime.now()));
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

    /**
     * Entrega una copia inmutable del historial para que la interfaz pueda
     * pintarlo sin depender de como se almacenan los datos.
     *
     * @return copia del historial, del mas antiguo al mas reciente
     */
    public List<RegistroEntrega> obtenerHistorial() {
        bloqueoHistorial.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(historial));
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
        registrarEnHistorial(new RegistroEntrega(pedido, REPARTIDOR_NO_ASIGNADO, LocalDateTime.now()));
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
        List<RegistroEntrega> registros = obtenerHistorial();
        if (registros.isEmpty()) {
            System.out.println("→ No hay entregas registradas aún.");
            return;
        }
        int total = 0;
        for (RegistroEntrega registro : registros) {
            if (!registro.getNombreRepartidor().equalsIgnoreCase(nombreRepartidor)) {
                continue;
            }
            total++;
            System.out.println(total + ". Pedido " + registro.getIdPedido()
                    + " | Tipo: " + registro.getTipoPedido()
                    + " | Tiempo: " + registro.getTiempoEntregaMin() + " min"
                    + " | Estado: " + registro.getEstado());
        }
        System.out.println("→ Total de entregas registradas: " + total);
    }
}
