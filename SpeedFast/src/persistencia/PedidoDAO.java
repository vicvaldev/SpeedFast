package persistencia;

import modelos.implementacion.EstadoPedido;
import modelos.implementacion.Pedido;
import modelos.implementacion.PedidoComida;
import modelos.implementacion.PedidoEncomienda;
import modelos.implementacion.PedidoExpress;
import modelos.implementacion.PedidoFabrica;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de la tabla `pedido`.
 *
 * <p>La jerarquia de Pedido es polimorfica, asi que la tabla tiene una
 * columna discriminadora (`tipo`) mas una columna nullable por atributo
 * propio de cada subclase. Al leer, {@link #crearPedido(ResultSet)}
 * reconstruye la subclase que corresponde.</p>
 *
 * <p>Las cuatro operaciones basicas siguen la convencion
 * create / readAll / update / delete. Los {@code read*} aceptan el filtro ya
 * resuelto y se apoyan en {@link #listar(String, Object...)}, que siempre
 * viaja con {@link PreparedStatement}: ningun valor chega concatenado al
 * SQL.</p>
 */
public class PedidoDAO {

    private static final String COLUMNAS =
            "id, direccion, tipo, estado, distancia_km, mochila_termica, peso, "
                    + "embalaje_validado, distancia_repartidor_cercano";

    private static final String SQL_INSERTAR = "INSERT INTO pedido "
            + "(id, direccion, tipo, estado, distancia_km, mochila_termica, peso, "
            + "embalaje_validado, distancia_repartidor_cercano) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR = "UPDATE pedido SET "
            + "direccion = ?, tipo = ?, estado = ?, distancia_km = ?, mochila_termica = ?, "
            + "peso = ?, embalaje_validado = ?, distancia_repartidor_cercano = ? "
            + "WHERE id = ?";

    private static final String SQL_SELECCIONAR = "SELECT " + COLUMNAS + " FROM pedido ";

    public PedidoDAO() {
    }

    /**
     * Inserta el pedido y devuelve el mismo objeto con el id generado por la
     * base de datos.
     */
    public Pedido create(Pedido pedido) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, pedido.getIdPedido());
            escribirDatos(pedido, ps, 2);
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    pedido.setIdPedido(claves.getInt(1));
                }
            }
        }
        return pedido;
    }

    public List<Pedido> readAll() throws SQLException {
        return listar(SQL_SELECCIONAR + "ORDER BY id");
    }

    public List<Pedido> readPendientes() throws SQLException {
        return listar(SQL_SELECCIONAR + "WHERE estado = ? ORDER BY id", EstadoPedido.PENDIENTE.name());
    }

    /**
     * Filtro opcional por tipo y por estado, combinables entre si.
     *
     * @param tipo   valor de `tipo`, o {@code null} para no filtrar
     * @param estado valor de `estado`, o {@code null} para no filtrar
     */
    public List<Pedido> readFiltrado(String tipo, EstadoPedido estado) throws SQLException {
        StringBuilder sql = new StringBuilder(SQL_SELECCIONAR);
        List<Object> parametros = new ArrayList<>();

        if (tipo != null) {
            sql.append("WHERE tipo = ? ");
            parametros.add(tipo);
        }
        if (estado != null) {
            sql.append(tipo != null ? "AND estado = ? " : "WHERE estado = ? ");
            parametros.add(estado.name());
        }
        sql.append("ORDER BY id");

        return listar(sql.toString(), parametros.toArray());
    }

    public Pedido readPorId(int idPedido) throws SQLException {
        List<Pedido> pedidos = listar(SQL_SELECCIONAR + "WHERE id = ?", idPedido);
        return pedidos.isEmpty() ? null : pedidos.get(0);
    }

    /**
     * Actualiza todos los datos del pedido, incluido su tipo, rebuilding las
     * columnas de detalle que correspondan a la subclase.
     */
    public void update(Pedido pedido) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(SQL_ACTUALIZAR)) {

            escribirDatos(pedido, ps, 1);
            ps.setInt(9, pedido.getIdPedido());
            ps.executeUpdate();
        }
    }

    /**
     * Cambia solo el estado del pedido abriendo su propia conexion, ya en
     * autocommit. Es la operacion que usa el reparto al iniciar el viaje.
     */
    public void update(int idPedido, EstadoPedido estado) throws SQLException {
        try (Connection conexion = ConexionDB.conectar()) {
            update(conexion, idPedido, estado);
        }
    }

    /**
     * Cambia solo el estado del pedido sobre una conexion entregada por el
     * llamador. Permite que el UPDATE participe de una transaccion mayor
     * (ver {@code ControladorDePedidos.registrarEntregaCompleta}): al no
     * cerrar la conexion ni cambiar su autocommit, el commit y el rollback
     * quedan en manos de quien la abrio.
     */
    public void update(Connection conexion, int idPedido, EstadoPedido estado) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("UPDATE pedido SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado.name());
            ps.setInt(2, idPedido);
            ps.executeUpdate();
        }
    }

    public void delete(int idPedido) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement("DELETE FROM pedido WHERE id = ?")) {
            ps.setInt(1, idPedido);
            ps.executeUpdate();
        }
    }

    public boolean existe(int idPedido) throws SQLException {
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement("SELECT 1 FROM pedido WHERE id = ?")) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Escribe las ocho columnas del pedido a partir de la jerarquia, dejando
     * en NULL (cero) las que no aplican a la subclase concreta.
     *
     * @param indice posicion del primer parametro en el PreparedStatement: 2 en
     *               el INSERT (el id ocupa la 1) y 1 en el UPDATE
     */
    private void escribirDatos(Pedido pedido, PreparedStatement ps, int indice) throws SQLException {
        ps.setString(indice, pedido.getDireccionEntrega());
        ps.setString(indice + 1, pedido.getTipoPedido());
        ps.setString(indice + 2, pedido.getEstado().name());
        ps.setDouble(indice + 3, pedido.getDistanciaKm());
        ps.setBoolean(indice + 4, pedido instanceof PedidoComida
                && ((PedidoComida) pedido).tieneMochilaTermica());
        ps.setDouble(indice + 5, pedido instanceof PedidoEncomienda
                ? ((PedidoEncomienda) pedido).getPeso() : 0.0);
        ps.setBoolean(indice + 6, pedido instanceof PedidoEncomienda
                && ((PedidoEncomienda) pedido).isEmbalajeValidado());
        ps.setDouble(indice + 7, pedido instanceof PedidoExpress
                ? ((PedidoExpress) pedido).getDistanciaRepartidorMasCercano() : 0.0);
    }

    private List<Pedido> listar(String sql, Object... parametros) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(crearPedido(rs));
                }
            }
        }
        return pedidos;
    }

    private Pedido crearPedido(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String direccion = rs.getString("direccion");
        double distanciaKm = rs.getDouble("distancia_km");

        String tipo = rs.getString("tipo");
        // Las columnas de detalle dependen de la subclase, asi que se piden solo
        // cuando el tipo las usa. La distancia del repartidor cercano es propia
        // de Express y el resto, null para el resto de los tipos.
        Pedido pedido;
        switch (tipo) {
            case PedidoFabrica.TIPO_COMIDA:
                pedido = new PedidoComida(id, direccion, distanciaKm,
                        leerBoolean(rs, "mochila_termica", true));
                break;
            case PedidoFabrica.TIPO_ENCOMIENDA:
                pedido = new PedidoEncomienda(id, direccion, distanciaKm,
                        leerDouble(rs, "peso", 5.0),
                        leerBoolean(rs, "embalaje_validado", true));
                break;
            default:
                // Una fila antigua con un tipo no mapeado se lee como Express:
                // es la subclase que no exige datos de detalle propios.
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
