package persistencia;

import modelos.implementacion.DetalleEntrega;
import modelos.implementacion.Entrega;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de la tabla `entrega`, que materializa la relacion entre un pedido y
 * el repartidor que lo realizo.
 *
 * <p>Los {@code read*} de {@link Entrega} devuelven solo las claves de la
 * tabla. Los {@code readDetalle*} parten de un JOIN con {@code pedido} y
 * {@code repartidor} y devuelven un {@link DetalleEntrega} con los textos ya
 * resueltos, que es lo que la tabla de la interfaz muestra.</p>
 */
public class EntregaDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO entrega (id_pedido, id_repartidor, fecha, hora) VALUES (?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE entrega SET id_pedido = ?, id_repartidor = ?, fecha = ?, hora = ? WHERE id = ?";

    private static final String SQL_SELECCIONAR =
            "SELECT id, id_pedido, id_repartidor, fecha, hora FROM entrega";

    private static final String SQL_DETALLE =
            "SELECT e.id, e.id_pedido, p.tipo, p.direccion, e.id_repartidor, r.nombre, e.fecha, e.hora "
                    + "FROM entrega e "
                    + "JOIN pedido p ON p.id = e.id_pedido "
                    + "JOIN repartidor r ON r.id = e.id_repartidor ";

    public EntregaDAO() {
    }

    /**
     * Inserta la entrega abriendo su propia conexion, ya en autocommit.
     *
     * @return el id generado para la entrega recien insertada
     */
    public int create(Entrega entrega) throws SQLException {
        try (Connection conexion = ConexionDB.conectar()) {
            return create(conexion, entrega);
        }
    }

    /**
     * Inserta la entrega sobre una conexion entregada por el llamador, de
     * modo que el INSERT pueda formar parte de una transaccion mayor (ver
     * {@code ControladorDePedidos.registrarEntregaCompleta}). La conexion no
     * se cierra ni se le cambia el autocommit: el commit y el rollback
     * quedan en manos de quien la abrio.
     *
     * @return el id generado para la entrega recien insertada
     */
    public int create(Connection conexion, Entrega entrega) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, entrega.getIdPedido());
            ps.setInt(2, entrega.getIdRepartidor());
            ps.setDate(3, Date.valueOf(entrega.getFecha()));
            ps.setTime(4, Time.valueOf(entrega.getHora()));
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    entrega.setId(claves.getInt(1));
                    return claves.getInt(1);
                }
            }
            return -1;
        }
    }

    public List<Entrega> readAll() throws SQLException {
        return leerEntregas(SQL_SELECCIONAR + " ORDER BY id");
    }

    public List<Entrega> readPorPedido(int idPedido) throws SQLException {
        return leerEntregas(SQL_SELECCIONAR + " WHERE id_pedido = ? ORDER BY id", idPedido);
    }

    public List<Entrega> readPorRepartidor(int idRepartidor) throws SQLException {
        return leerEntregas(SQL_SELECCIONAR + " WHERE id_repartidor = ? ORDER BY id", idRepartidor);
    }

    /**
     * Reasigna la entrega a otro pedido o repartidor, o le corrige la fecha y la
     * hora. El id del propio objeto indica que fila se modifica.
     */
    public void update(Entrega entrega) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(SQL_ACTUALIZAR)) {

            ps.setInt(1, entrega.getIdPedido());
            ps.setInt(2, entrega.getIdRepartidor());
            ps.setDate(3, Date.valueOf(entrega.getFecha()));
            ps.setTime(4, Time.valueOf(entrega.getHora()));
            ps.setInt(5, entrega.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int idEntrega) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement("DELETE FROM entrega WHERE id = ?")) {
            ps.setInt(1, idEntrega);
            ps.executeUpdate();
        }
    }

    /**
     * Cuenta las entregas de un pedido o de un repartidor. La vista lo usa
     * para avisar cuantas filas se perderan al borrar en cascada.
     */
    public int contarPorPedido(int idPedido) throws SQLException {
        return contar("SELECT COUNT(*) FROM entrega WHERE id_pedido = ?", idPedido);
    }

    public int contarPorRepartidor(int idRepartidor) throws SQLException {
        return contar("SELECT COUNT(*) FROM entrega WHERE id_repartidor = ?", idRepartidor);
    }

    public List<DetalleEntrega> readDetalle() throws SQLException {
        return leerDetalle("");
    }

    public List<DetalleEntrega> readDetallePorPedido(int idPedido) throws SQLException {
        return leerDetalle("WHERE e.id_pedido = ? ", idPedido);
    }

    public List<DetalleEntrega> readDetallePorRepartidor(int idRepartidor) throws SQLException {
        return leerDetalle("WHERE e.id_repartidor = ? ", idRepartidor);
    }

    private List<DetalleEntrega> leerDetalle(String where, Object... parametros) throws SQLException {
        List<DetalleEntrega> entregas = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(SQL_DETALLE + where + "ORDER BY e.id")) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entregas.add(new DetalleEntrega(
                            rs.getInt("id"),
                            rs.getInt("id_pedido"),
                            rs.getString("tipo"),
                            rs.getString("direccion"),
                            rs.getInt("id_repartidor"),
                            rs.getString("nombre"),
                            rs.getDate("fecha").toLocalDate(),
                            rs.getTime("hora").toLocalTime()));
                }
            }
        }
        return entregas;
    }

    private List<Entrega> leerEntregas(String sql, Object... parametros) throws SQLException {
        List<Entrega> entregas = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entregas.add(crearEntrega(rs));
                }
            }
        }
        return entregas;
    }

    private int contar(String sql, int valor) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Entrega crearEntrega(ResultSet rs) throws SQLException {
        return new Entrega(
                rs.getInt("id"),
                rs.getInt("id_pedido"),
                rs.getInt("id_repartidor"),
                rs.getDate("fecha").toLocalDate(),
                rs.getTime("hora").toLocalTime());
    }
}
