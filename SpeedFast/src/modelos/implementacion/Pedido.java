package modelos.implementacion;

public abstract class Pedido {
    private static final long FACTOR_PAUSA_MS = 150L;

    private int idPedido;
    private String direccionEntrega;
    private double distanciaKm;
    private String tipoPedido;
    private EstadoPedido estado;

    public Pedido(int idPedido, String direccionEntrega, double distanciaKm, String tipoPedido) {
        this.idPedido = idPedido;
        this.direccionEntrega = direccionEntrega;
        this.distanciaKm = distanciaKm;
        this.tipoPedido = tipoPedido;
        this.estado = EstadoPedido.PENDIENTE;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(double distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    public String getTipoPedido() {
        return tipoPedido;
    }

    public void setTipoPedido(String tipoPedido) {
        this.tipoPedido = tipoPedido;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public void setEstado(String nuevoEstado) {
        this.estado = EstadoPedido.valueOf(nuevoEstado);
    }

    @Override
    public String toString() {
        return "Pedido #" + idPedido + " | " + tipoPedido + " | Destino: "
                + direccionEntrega + " | Estado: " + estado;
    }

    public void mostrarResumen() {
        System.out.println("=== Resumen del Pedido ===");
        System.out.println("ID Pedido:      " + idPedido);
        System.out.println("Tipo:           " + tipoPedido);
        System.out.println("Direccion:      " + direccionEntrega);
        System.out.println("Distancia:      " + distanciaKm + " km");
    }

    public abstract double calcularTiempoEntrega();

    public long calcularPausaEntregaMs() {
        return Math.round(calcularTiempoEntrega()) * FACTOR_PAUSA_MS + (idPedido % 5) * 130L;
    }

    public abstract boolean validarEntrega();

    public void asignarRepartidor() {
        System.out.println("Asignando repartidor...");
        if (validarEntrega()) {
            setEstado(EstadoPedido.EN_REPARTO);
            System.out.println("→ Repartidor asignado automáticamente. Entrega iniciada.");
        } else {
            System.out.println("→ No se pudo iniciar la entrega: el pedido no cumple las condiciones.");
        }
    }

    public void asignarRepartidor(String nombreRepartidor) {
        System.out.println("Asignando repartidor...");
        if (validarEntrega()) {
            setEstado(EstadoPedido.EN_REPARTO);
            System.out.println("→ Pedido asignado a " + nombreRepartidor + ". Entrega iniciada.");
        } else {
            System.out.println("→ No se pudo iniciar la entrega: el pedido no cumple las condiciones.");
        }
    }
}
