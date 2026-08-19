package modelos;

public class PedidoComida extends Pedido {
    private boolean mochilaTermica;

    public PedidoComida(int idPedido, String direccionEntrega, double distanciaKm, boolean mochilaTermica) {
        super(idPedido, direccionEntrega, distanciaKm, "Comida");
        this.mochilaTermica = mochilaTermica;
    }

    public boolean tieneMochilaTermica() {
        return mochilaTermica;
    }

    @Override
    public double calcularTiempoEntrega() {
        return 15 + (2 * getDistanciaKm());
    }

    @Override
    public void asignarRepartidor() {
        System.out.println("Asignando repartidor...");
        String estado = mochilaTermica ? "OK" : "NO OK";
        System.out.println("→ Verificando mochila térmica... " + estado);
        System.out.println("→ Repartidor asignado automáticamente.");
    }

    @Override
    public void asignarRepartidor(String nombreRepartidor) {
        System.out.println("Asignando repartidor...");
        String estado = mochilaTermica ? "OK" : "NO OK";
        System.out.println("→ Verificando mochila térmica... " + estado);
        System.out.println("→ Pedido asignado a " + nombreRepartidor);
    }
}
