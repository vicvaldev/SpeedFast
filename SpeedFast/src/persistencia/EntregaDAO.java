package persistencia;

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
 * Acceso a datos de la tabla `entrega`, que materializa la relacion
 * entre un pedido y el repartidor que lo realizo.
 */
public class EntregaDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO entrega (id_pedido, id_repartidor, fecha, hora) VALUES (?, ?, ?, ?)";

    private static final String SQL_SELECCIONAR =
            "SELECT id, id_pedido, id_repartidor, fecha, hora FROM entrega ORDER BY id";

    public EntregaDAO() {
    }

    /**
     * @return el id generado para la entrega recien insertada
     */
    public int guardar(Entrega entrega) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

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

    public List<Entrega> listarTodas() throws SQLException {
        List<Entrega> entregas = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(SQL_SELECCIONAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                entregas.add(crearEntrega(rs));
            }
        }
        return entregas;
    }

    public List<Entrega> listarPorPedido(int idPedido) throws SQLException {
        String sql = "SELECT id, id_pedido, id_repartidor, fecha, hora FROM entrega "
                + "WHERE id_pedido = ? ORDER BY id";
        List<Entrega> entregas = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entregas.add(crearEntrega(rs));
                }
            }
        }
        return entregas;
    }

    private Entrega crearEntrega(ResultSet rs) throws SQLException {
        Entrega entrega = new Entrega(
                rs.getInt("id_pedido"),
                rs.getInt("id_repartidor"),
                rs.getDate("fecha").toLocalDate(),
                rs.getTime("hora").toLocalTime());
        entrega.setId(rs.getInt("id"));
        return entrega;
    }
}
