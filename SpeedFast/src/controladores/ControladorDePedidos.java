package controladores;

import modelos.implementacion.EstadoPedido;
import modelos.implementacion.Pedido;
import modelos.implementacion.Repartidor;
import persistencia.ConexionDB;
import persistencia.Entrega;
import persistencia.EntregaDAO;
import persistencia.PedidoDAO;
import persistencia.RepartidorDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Controlador especifico que concentra el acceso a los datos de pedidos,
 * repartidores y entregas.
 *
 * <p>Es el unico punto por el que la capa de vistas obtiene informacion
 * persistida. Las ventanas reciben esta clase por constructor y nunca
 * construyen un DAO ni conocen el paquete {@code persistencia}, de modo que
 * cambiar el mecanismo de almacenamiento (MySQL, archivos, memoria) solo
 * obliga a modificar este controlador.</p>
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

    public List<Pedido> listarTodos() throws SQLException {
        return pedidoDAO.listarTodos();
    }

    public List<Pedido> listarPendientes() throws SQLException {
        return pedidoDAO.listarPendientes();
    }

    public void guardar(Pedido pedido) throws SQLException {
        pedidoDAO.guardar(pedido);
    }

    public void actualizarEstado(int idPedido, EstadoPedido estado) throws SQLException {
        pedidoDAO.actualizarEstado(idPedido, estado);
    }

    public boolean existeId(int idPedido) throws SQLException {
        return pedidoDAO.existeId(idPedido);
    }

    public List<Repartidor> listarRepartidores() throws SQLException {
        return repartidorDAO.listarTodos();
    }

    public void guardarRepartidor(Repartidor repartidor) throws SQLException {
        repartidorDAO.guardar(repartidor);
    }

    public boolean existeNombreRepartidor(String nombre) throws SQLException {
        return repartidorDAO.existeNombre(nombre);
    }

    public void registrarEntregaEnBD(int idPedido, int idRepartidor) throws SQLException {
        entregaDAO.guardar(new Entrega(idPedido, idRepartidor));
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
                pedidoDAO.actualizarEstado(conexion, idPedido, EstadoPedido.ENTREGADO);
                entregaDAO.guardar(conexion, entrega);
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
