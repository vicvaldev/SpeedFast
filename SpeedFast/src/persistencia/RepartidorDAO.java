package persistencia;

import modelos.implementacion.Repartidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a datos de la tabla `repartidor`.
 */
public class RepartidorDAO {

    private static final String SQL_INSERTAR = "INSERT INTO repartidor (nombre) VALUES (?)";
    private static final String SQL_SELECCIONAR = "SELECT id, nombre FROM repartidor ORDER BY nombre";

    public RepartidorDAO() {
    }

    public void guardar(Repartidor repartidor) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, repartidor.getNombre());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    repartidor.setIdRepartidor(claves.getInt(1));
                }
            }
        }
    }

    public List<Repartidor> listarTodos() throws SQLException {
        List<Repartidor> repartidores = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(SQL_SELECCIONAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                repartidores.add(crearRepartidor(rs));
            }
        }
        return repartidores;
    }

    public boolean existeNombre(String nombre) throws SQLException {
        String sql = "SELECT 1 FROM repartidor WHERE nombre = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Repartidor crearRepartidor(ResultSet rs) throws SQLException {
        return new Repartidor(rs.getInt("id"), rs.getString("nombre"));
    }
}
