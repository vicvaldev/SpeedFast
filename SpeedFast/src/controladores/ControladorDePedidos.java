package controladores;

import modelos.implementacion.DetalleEntrega;
import modelos.implementacion.Entrega;
import modelos.implementacion.EstadoPedido;
import modelos.implementacion.Pedido;
import modelos.implementacion.Repartidor;
import persistencia.ConexionDB;
import persistencia.EntregaDAO;
import persistencia.PedidoDAO;
import persistencia.RepartidorDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Fachada de pedidos, repartidores y entregas.
 *
 * <p>Es el unico punto por el que la capa de vistas obtiene informacion
 * persistida. Las ventanas reciben esta clase por constructor y nunca
 * construyen un DAO ni conocen el paquete {@code persistencia}, de modo que
 * cambiar el mecanismo de almacenamiento (MySQL, archivos, memoria) solo
 * obliga a modificar este controlador.</p>
 *
 * <p>Aqui viven las cuatro operaciones del CRUD para las tres entidades. Los
 * DAO usan el nombre de la convencion (create / readAll / update / delete)
 * porque es la que exige la especificacion; esta fachada las expone en espanol
 * para que las vistas lean como el resto de la aplicacion.</p>
 *
 * <p>Los metodos propagan {@link SQLException} para que cada vista conserve
 * sus propios mensajes de error, y todos son bloqueantes: el llamador
 * responsable debe ejecutarlos fuera del Event Dispatch Thread.</p>
 *
 * <p>Cuando una accion de negocio modifica mas de una tabla, el controlador
 * agrupa las escrituras en una transaccion explicita en vez de delegarlas en
 * DAO separados: asi comparte una unica conexion y confirma o deshace el
 * conjunto completo (ver {@link #registrarEntregaCompleta}).</p>
 */
public class ControladorDePedidos {

    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final RepartidorDAO repartidorDAO = new RepartidorDAO();
    private final EntregaDAO entregaDAO = new EntregaDAO();

    // ---------------------------------------------------------------
    // Pedidos
    // ---------------------------------------------------------------

    /**
     * @return el pedido guardado, con el id que le asigno la base de datos
     */
    public Pedido crearPedido(Pedido pedido) throws SQLException {
        return pedidoDAO.create(pedido);
    }

    public List<Pedido> listarPedidos() throws SQLException {
        return pedidoDAO.readAll();
    }

    public List<Pedido> listarPedidosPendientes() throws SQLException {
        return pedidoDAO.readPendientes();
    }

    /**
     * @param tipo   tipo a filtrar, o {@code null} para no filtrar por tipo
     * @param estado estado a filtrar, o {@code null} para no filtrar por estado
     */
    public List<Pedido> listarPedidosFiltrados(String tipo, EstadoPedido estado) throws SQLException {
        return pedidoDAO.readFiltrado(tipo, estado);
    }

    public Pedido obtenerPedido(int idPedido) throws SQLException {
        return pedidoDAO.readPorId(idPedido);
    }

    public void actualizarPedido(Pedido pedido) throws SQLException {
        pedidoDAO.update(pedido);
    }

    /**
     * Elimina el pedido. Las entregas registradas se borran en cascada por la
     * FK {@code fk_entrega_pedido}.
     */
    public void eliminarPedido(int idPedido) throws SQLException {
        pedidoDAO.delete(idPedido);
    }

    public void cambiarEstadoPedido(int idPedido, EstadoPedido estado) throws SQLException {
        pedidoDAO.update(idPedido, estado);
    }

    public boolean existePedido(int idPedido) throws SQLException {
        return pedidoDAO.existe(idPedido);
    }

    /** @return cuantas entregas tiene registradas el pedido */
    public int cantidadEntregasDePedido(int idPedido) throws SQLException {
        return entregaDAO.contarPorPedido(idPedido);
    }

    // ---------------------------------------------------------------
    // Repartidores
    // ---------------------------------------------------------------

    /**
     * @return el repartidor guardado, con el id que le asigno la base de datos
     */
    public Repartidor crearRepartidor(Repartidor repartidor) throws SQLException {
        return repartidorDAO.create(repartidor);
    }

    public List<Repartidor> listarRepartidores() throws SQLException {
        return repartidorDAO.readAll();
    }

    public void actualizarRepartidor(Repartidor repartidor) throws SQLException {
        repartidorDAO.update(repartidor);
    }

    /**
     * Elimina el repartidor. Sus entregas se borran en cascada por la FK
     * {@code fk_entrega_repartidor}.
     */
    public void eliminarRepartidor(int idRepartidor) throws SQLException {
        repartidorDAO.delete(idRepartidor);
    }

    /**
     * @param idExcluido repartidor a ignorar al buscar duplicados; 0 en el alta
     */
    public boolean existeNombreRepartidor(String nombre, int idExcluido) throws SQLException {
        return repartidorDAO.existeNombre(nombre, idExcluido);
    }

    /** @return cuantas entregas tiene registradas el repartidor */
    public int cantidadEntregasDeRepartidor(int idRepartidor) throws SQLException {
        return entregaDAO.contarPorRepartidor(idRepartidor);
    }

    // ---------------------------------------------------------------
    // Entregas
    // ---------------------------------------------------------------

    /**
     * @return el id generado para la entrega recien insertada
     */
    public int crearEntrega(Entrega entrega) throws SQLException {
        return entregaDAO.create(entrega);
    }

    public void actualizarEntrega(Entrega entrega) throws SQLException {
        entregaDAO.update(entrega);
    }

    public void eliminarEntrega(int idEntrega) throws SQLException {
        entregaDAO.delete(idEntrega);
    }

    public List<DetalleEntrega> listarEntregas() throws SQLException {
        return entregaDAO.readDetalle();
    }

    public List<DetalleEntrega> listarEntregasDePedido(int idPedido) throws SQLException {
        return entregaDAO.readDetallePorPedido(idPedido);
    }

    public List<DetalleEntrega> listarEntregasDeRepartidor(int idRepartidor) throws SQLException {
        return entregaDAO.readDetallePorRepartidor(idRepartidor);
    }

    /**
     * Cierra un pedido como entregado y registra su entrega en una sola
     * transaccion.
     *
     * <p>Son dos escrituras sobre tablas distintas que describe un unico
     * hecho de negocio: si el UPDATE del estado se confirmara y el INSERT de
     * la entrega fallara, el pedido quedaria ENTREGADO sin reparto asociado y
     * la entrega existira sin estado coherente. Al compartir conexion,
     * desactivar el autocommit y confirmar (o deshacer) juntas, el par de
     * operaciones es atomico.</p>
     *
     * <p>Ante cualquier fallo se deshace todo y la excepcion original se
     * propaga para que la vista muestre su propio mensaje de error.</p>
     *
     * @throws SQLException si alguna de las dos escrituras falla
     */
    public void registrarEntregaCompleta(int idPedido, int idRepartidor) throws SQLException {
        Entrega entrega = new Entrega(idPedido, idRepartidor);
        try (Connection conexion = ConexionDB.conectar()) {
            conexion.setAutoCommit(false);
            try {
                pedidoDAO.update(conexion, idPedido, EstadoPedido.ENTREGADO);
                entregaDAO.create(conexion, entrega);
                conexion.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    /**
     * @return {@code true} si la base de datos esta disponible
     */
    public boolean verificarDisponibilidad() {
        return ConexionDB.verificarConexion();
    }
}
