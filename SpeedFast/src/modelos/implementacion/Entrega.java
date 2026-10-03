package modelos.implementacion;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Registra la relacion entre un pedido y el repartidor que lo llevo.
 * La tabla `entrega` separa la marca de tiempo en dos columnas
 * (fecha DATE y hora TIME), por eso el DTO tambien las separa.
 *
 * <p>Es una entidad del dominio, no un detalle de la capa de datos: la usan
 * por igual la fachada de {@code controladores}, el DAO que la persiste y la
 * vista que la registra desde un formulario.</p>
 */
public class Entrega {
    private int id;
    private int idPedido;
    private int idRepartidor;
    private LocalDate fecha;
    private LocalTime hora;

    public Entrega(int idPedido, int idRepartidor, LocalDate fecha, LocalTime hora) {
        this.idPedido = idPedido;
        this.idRepartidor = idRepartidor;
        this.fecha = fecha;
        this.hora = hora;
    }

    /**
     * Entrega ya persistida: el id permite localizarla en la tabla `entrega`
     * para actualizarla o eliminarla.
     */
    public Entrega(int id, int idPedido, int idRepartidor, LocalDate fecha, LocalTime hora) {
        this.id = id;
        this.idPedido = idPedido;
        this.idRepartidor = idRepartidor;
        this.fecha = fecha;
        this.hora = hora;
    }

    public Entrega(int idPedido, int idRepartidor) {
        this(idPedido, idRepartidor, LocalDate.now(), LocalTime.now());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public int getIdRepartidor() {
        return idRepartidor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    @Override
    public String toString() {
        return "Entrega #" + id + " | Pedido: " + idPedido
                + " | Repartidor: " + idRepartidor
                + " | " + fecha + " " + hora;
    }
}
