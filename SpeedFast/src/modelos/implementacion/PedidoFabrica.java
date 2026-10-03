package modelos.implementacion;

/**
 * Fabrica de pedidos: decide que subclase de {@link Pedido} corresponde a un
 * tipo y le aplica los valores por defecto de sus datos de detalle.
 *
 * <p>Centraliza el reparto por tipo que antes estaba duplicado entre el
 * formulario de registro y el {@code PedidoDAO} al leer de la base de datos, de
 * modo que un pedido creado desde la interfaz y uno reconstruido desde una fila
 * queden siempre con los mismos valores.</p>
 */
public final class PedidoFabrica {

    public static final String TIPO_COMIDA = "Comida";
    public static final String TIPO_ENCOMIENDA = "Encomienda";
    public static final String TIPO_EXPRESS = "Express";

    // Valores por defecto de los atributos propios de cada subclase. Son los
    // que hacen que un pedido pase validarEntrega(): sin mochila termica una
    // comida no se despacha, y una encomienda necesita peso <= 20 kg con
    // embalaje validado.
    private static final boolean MOCHILA_TERMICA_POR_DEFECTO = true;
    private static final double PESO_POR_DEFECTO = 5.0;
    private static final boolean EMBALAJE_VALIDADO_POR_DEFECTO = true;
    private static final double DISTANCIA_REPARTIDOR_POR_DEFECTO = 1.0;

    private PedidoFabrica() {
    }

    /**
     * @return {@code true} si el tipo corresponde a alguna subclase de Pedido
     */
    public static boolean esTipoConocido(String tipo) {
        return TIPO_COMIDA.equals(tipo)
                || TIPO_ENCOMIENDA.equals(tipo)
                || TIPO_EXPRESS.equals(tipo);
    }

    /**
     * Crea el pedido del tipo indicado, con los datos de detalle por defecto.
     *
     * @throws IllegalArgumentException si el tipo no corresponde a ninguna subclase
     */
    public static Pedido crear(String tipo, int idPedido, String direccion, double distanciaKm) {
        switch (tipo) {
            case TIPO_COMIDA:
                return new PedidoComida(idPedido, direccion, distanciaKm, MOCHILA_TERMICA_POR_DEFECTO);
            case TIPO_ENCOMIENDA:
                return new PedidoEncomienda(idPedido, direccion, distanciaKm,
                        PESO_POR_DEFECTO, EMBALAJE_VALIDADO_POR_DEFECTO);
            case TIPO_EXPRESS:
                return new PedidoExpress(idPedido, direccion, distanciaKm,
                        DISTANCIA_REPARTIDOR_POR_DEFECTO);
            default:
                throw new IllegalArgumentException("Tipo de pedido desconocido: " + tipo);
        }
    }
}
