package persistencia;

import modelos.implementacion.EstadoPedido;
import modelos.implementacion.Pedido;
import modelos.implementacion.PedidoComida;
import modelos.implementacion.PedidoEncomienda;
import modelos.implementacion.PedidoExpress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a datos de la tabla `pedido`.
 *
 * <p>La jerarquia de Pedido es polimorfica, asi que la tabla tiene una
 * columna discriminadora (`tipo`) mas una columna nullable por atributo
 * propio de cada subclase. Al leer, {@link #crearPedido(ResultSet)}
 * reconstruye la subclase que corresponde.</p>
 */
public class PedidoDAO {

    private static final String SQL_INSERTAR = "INSERT INTO pedido "
            + "(id, direccion, tipo, estado, distancia_km, mochila_termica, peso, "
            + "embalaje_validado, distancia_repartidor_cercano) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECCIONAR = "SELECT id, direccion, tipo, estado, "
            + "distancia_km, mochila_termica, peso, embalaje_validado, distancia_repartidor_cercano "
            + "FROM pedido ";

    public PedidoDAO() {
    }

    public void guardar(Pedido pedido) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, pedido.getIdPedido());
            ps.setString(2, pedido.getDireccionEntrega());
            ps.setString(3, pedido.getTipoPedido());
            ps.setString(4, pedido.getEstado().name());
            ps.setDouble(5, pedido.getDistanciaKm());
            // Las columnas de detalle no aplican a todas las subclases: se dejan
            // en NULL cuando el pedido no es de ese tipo.
            ps.setBoolean(6, pedido instanceof PedidoComida
                    && ((PedidoComida) pedido).tieneMochilaTermica());
            ps.setDouble(7, pedido instanceof PedidoEncomienda
                    ? ((PedidoEncomienda) pedido).getPeso() : 0.0);
            ps.setBoolean(8, pedido instanceof PedidoEncomienda
                    && ((PedidoEncomienda) pedido).isEmbalajeValidado());
            ps.setDouble(9, pedido instanceof PedidoExpress
                    ? ((PedidoExpress) pedido).getDistanciaRepartidorMasCercano() : 0.0);

            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    pedido.setIdPedido(claves.getInt(1));
                }
            }
        }
    }

    public List<Pedido> listarTodos() throws SQLException {
        return listar(SQL_SELECCIONAR + "ORDER BY id");
    }

    public List<Pedido> listarPendientes() throws SQLException {
        return listar(SQL_SELECCIONAR + "WHERE estado = '"
                + EstadoPedido.PENDIENTE.name() + "' ORDER BY id");
    }

    private List<Pedido> listar(String sql) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pedidos.add(crearPedido(rs));
            }
        }
        return pedidos;
    }

    public void actualizarEstado(int idPedido, EstadoPedido estado) throws SQLException {
        String sql = "UPDATE pedido SET estado = ? WHERE id = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, idPedido);
            ps.executeUpdate();
        }
    }

    public boolean existeId(int idPedido) throws SQLException {
        String sql = "SELECT 1 FROM pedido WHERE id = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void eliminar(int idPedido) throws SQLException {
        String sql = "DELETE FROM pedido WHERE id = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            ps.executeUpdate();
        }
    }

    private Pedido crearPedido(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String direccion = rs.getString("direccion");
        String tipo = rs.getString("tipo");
        double distanciaKm = rs.getDouble("distancia_km");

        Pedido pedido;
        switch (tipo) {
            case "Comida":
                pedido = new PedidoComida(id, direccion, distanciaKm,
                        leerBoolean(rs, "mochila_termica", true));
                break;
            case "Encomienda":
                pedido = new PedidoEncomienda(id, direccion, distanciaKm,
                        leerDouble(rs, "peso", 5.0),
                        leerBoolean(rs, "embalaje_validado", true));
                break;
            default:
                pedido = new PedidoExpress(id, direccion, distanciaKm,
                        leerDouble(rs, "distancia_repartidor_cercano", 1.0));
                break;
        }

        pedido.setEstado(rs.getString("estado"));
        return pedido;
    }

    // Las columnas de detalle son nullable: un NULL se interpreta con el mismo
    // valor que aplica el formulario al crear el pedido, para que el round-trip
    // no termine rechazando la entrega en validarEntrega().
    private boolean leerBoolean(ResultSet rs, String columna, boolean porDefecto) throws SQLException {
        boolean valor = rs.getBoolean(columna);
        return rs.getObject(columna) == null ? porDefecto : valor;
    }

    private double leerDouble(ResultSet rs, String columna, double porDefecto) throws SQLException {
        double valor = rs.getDouble(columna);
        return rs.getObject(columna) == null ? porDefecto : valor;
    }
}
