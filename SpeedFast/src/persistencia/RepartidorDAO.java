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
 * CRUD de la tabla `repartidor`.
 */
public class RepartidorDAO {

    private static final String SQL_INSERTAR = "INSERT INTO repartidor (nombre) VALUES (?)";
    private static final String SQL_ACTUALIZAR = "UPDATE repartidor SET nombre = ? WHERE id = ?";
    private static final String SQL_SELECCIONAR = "SELECT id, nombre FROM repartidor ORDER BY nombre";

    public RepartidorDAO() {
    }

    /**
     * Inserta el repartidor y devuelve el mismo objeto con el id generado por
     * la base de datos.
     */
    public Repartidor create(Repartidor repartidor) throws SQLException {
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
        return repartidor;
    }

    public List<Repartidor> readAll() throws SQLException {
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

    /**
     * Renombra el repartidor y refleja el cambio en el objeto recibido, para
     * que la vista pueda repintar la fila sin volver a consultar.
     */
    public void update(Repartidor repartidor) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(SQL_ACTUALIZAR)) {

            ps.setString(1, repartidor.getNombre());
            ps.setInt(2, repartidor.getIdRepartidor());
            ps.executeUpdate();
        }
    }

    /**
     * Elimina el repartidor. Las entregas que tiene registradas se borran en
     * cascada por la FK {@code fk_entrega_repartidor}.
     */
    public void delete(int idRepartidor) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement("DELETE FROM repartidor WHERE id = ?")) {
            ps.setInt(1, idRepartidor);
            ps.executeUpdate();
        }
    }

    /**
     * Indica si ya existe un repartidor con ese nombre.
     *
     * @param idExcluido id a ignorar: al editar se pasa el id del propio
     *                   repartidor, para que mantener su nombre no se reporte
     *                   como duplicado. En el alta se pasa 0.
     */
    public boolean existeNombre(String nombre, int idExcluido) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(
                     "SELECT 1 FROM repartidor WHERE nombre = ? AND id <> ?")) {
            ps.setString(1, nombre);
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Repartidor crearRepartidor(ResultSet rs) throws SQLException {
        return new Repartidor(rs.getInt("id"), rs.getString("nombre"));
    }
}
