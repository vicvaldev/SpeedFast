package modelos;

public class PedidoComida extends Pedido {
    private boolean mochilaTermica;

    public PedidoComida(int idPedido, String direccionEntrega, boolean mochilaTermica) {
        super(idPedido, direccionEntrega, "Comida");
        this.mochilaTermica = mochilaTermica;
    }

    public boolean tieneMochilaTermica() {
        return mochilaTermica;
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
