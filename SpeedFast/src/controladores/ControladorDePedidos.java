package controladores;

import modelos.implementacion.EstadoPedido;
import modelos.implementacion.Pedido;
import modelos.implementacion.Repartidor;
import persistencia.ConexionDB;
import persistencia.Entrega;
import persistencia.EntregaDAO;
import persistencia.PedidoDAO;
import persistencia.RepartidorDAO;

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
     * @return {@code true} si la base de datos esta disponible
     */
    public boolean verificarDisponibilidad() {
        return ConexionDB.verificarConexion();
    }
}
