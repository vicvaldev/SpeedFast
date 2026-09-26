package persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Punto unico de acceso a la base de datos MySQL del proyecto.
 *
 * <p>Las vistas no construyen conexiones y los modelos no conocen
 * credenciales: toda la persistencia queda encapsulada aqui y en los DAO.</p>
 */
public class ConexionDB {

    // allowPublicKeyRetrieval=true es obligatorio porque el usuario root usa
    // caching_sha2_password y la conexion se hace por TCP sin SSL; sin este
    // parametro el driver lanza "Public Key Retrieval is not allowed".
    private static final String URL = "jdbc:mysql://localhost:3306/speedfast_db"
            + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=America/Santiago";
    private static final String USER = "root";
    private static final String PASSWORD = "desarrollo";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "No se encontro el driver MySQL. Verifica que lib/mysql-connector-j-26.7.0.jar "
                            + "este agregado como libreria del modulo.");
        }
    }

    private ConexionDB() {
    }

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static boolean verificarConexion() {
        try (Connection conexion = conectar()) {
            return conexion.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }
}
