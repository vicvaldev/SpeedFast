package modelos;

public class PedidoEncomienda extends Pedido {
    private double peso;
    private boolean embalajeValidado;

    public PedidoEncomienda(int idPedido, String direccionEntrega, double peso, boolean embalajeValidado) {
        super(idPedido, direccionEntrega, "Encomienda");
        this.peso = peso;
        this.embalajeValidado = embalajeValidado;
    }

    public double getPeso() {
        return peso;
    }

    public boolean isEmbalajeValidado() {
        return embalajeValidado;
    }

    @Override
    public void asignarRepartidor() {
        System.out.println("Asignando repartidor...");
        String estado = (embalajeValidado && peso <= 20.0) ? "OK" : "NO OK";
        System.out.println("→ Validando peso y embalaje... " + estado);
        System.out.println("→ Repartidor asignado automáticamente.");
    }

    @Override
    public void asignarRepartidor(String nombreRepartidor) {
        System.out.println("Asignando repartidor...");
        String estado = (embalajeValidado && peso <= 20.0) ? "OK" : "NO OK";
        System.out.println("→ Validando peso y embalaje... " + estado);
        System.out.println("→ Pedido asignado a " + nombreRepartidor);
    }
}
