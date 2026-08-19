package modelos;

public abstract class Pedido {
    private int idPedido;
    private String direccionEntrega;
    private double distanciaKm;
    private String tipoPedido;

    public Pedido(int idPedido, String direccionEntrega, double distanciaKm, String tipoPedido) {
        this.idPedido = idPedido;
        this.direccionEntrega = direccionEntrega;
        this.distanciaKm = distanciaKm;
        this.tipoPedido = tipoPedido;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    public String getTipoPedido() {
        return tipoPedido;
    }

    public void mostrarResumen() {
        System.out.println("=== Resumen del Pedido ===");
        System.out.println("ID Pedido:      " + idPedido);
        System.out.println("Tipo:           " + tipoPedido);
        System.out.println("Direccion:      " + direccionEntrega);
        System.out.println("Distancia:      " + distanciaKm + " km");
    }

    public abstract double calcularTiempoEntrega();

    public void asignarRepartidor() {
        System.out.println("Asignando repartidor...");
        System.out.println("→ Repartidor asignado automáticamente.");
    }

    public void asignarRepartidor(String nombreRepartidor) {
        System.out.println("Asignando repartidor...");
        System.out.println("→ Pedido asignado a " + nombreRepartidor);
    }
}
