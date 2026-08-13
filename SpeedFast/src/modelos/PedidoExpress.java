package modelos;

public class PedidoExpress extends Pedido {
    private double distanciaRepartidorMasCercano;

    public PedidoExpress(int idPedido, String direccionEntrega, double distanciaRepartidorMasCercano) {
        super(idPedido, direccionEntrega, "Compra Express");
        this.distanciaRepartidorMasCercano = distanciaRepartidorMasCercano;
    }

    public double getDistanciaRepartidorMasCercano() {
        return distanciaRepartidorMasCercano;
    }

    @Override
    public void asignarRepartidor() {
        System.out.println("Asignando repartidor...");
        System.out.println("→ Repartidor más cercano con disponibilidad inmediata encontrado.");
        System.out.println("→ Repartidor asignado automáticamente.");
    }

    @Override
    public void asignarRepartidor(String nombreRepartidor) {
        System.out.println("Asignando repartidor...");
        System.out.println("→ Repartidor más cercano con disponibilidad inmediata encontrado.");
        System.out.println("→ Pedido asignado a " + nombreRepartidor);
    }
}
