package vistas;

import modelos.implementacion.ControladorDeEnvios;
import modelos.implementacion.RegistroEntrega;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Muestra el historial de entregas acumulado durante la sesion.
 *
 * <p>A diferencia del resto de ventanas, no consulta la base de datos: lee un
 * snapshot del {@link ControladorDeEnvios}, por lo que no necesita ejecutarse
 * fuera del Event Dispatch Thread.</p>
 */
public class VentanaHistorialEntregas extends JFrame {

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final VentanaPrincipal principal;
    private final ControladorDeEnvios controladorEnvios;
    private final Runnable actualizador;
    private final DefaultTableModel modelo;
    private final JLabel contador;

    public VentanaHistorialEntregas(VentanaPrincipal principal, ControladorDeEnvios controladorEnvios) {
        this.principal = principal;
        this.controladorEnvios = controladorEnvios;
        this.actualizador = this::actualizarTabla;

        setTitle("Historial de Entregas");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(720, 320);
        setLocationRelativeTo(principal);
        setLayout(new BorderLayout());

        modelo = new DefaultTableModel(
                new String[]{"#", "Pedido", "Tipo", "Dirección", "Repartidor", "Tiempo (min)", "Estado", "Fecha y hora"},
                0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new BorderLayout());
        panelBotones.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> actualizarTabla());
        panelBotones.add(btnRefrescar, BorderLayout.WEST);

        contador = new JLabel(" ");
        contador.setHorizontalAlignment(SwingConstants.RIGHT);
        panelBotones.add(contador, BorderLayout.EAST);

        add(panelBotones, BorderLayout.SOUTH);

        principal.agregarActualizador(actualizador);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                principal.quitarActualizador(actualizador);
            }
        });

        actualizarTabla();
    }

    private void actualizarTabla() {
        List<RegistroEntrega> registros = controladorEnvios.obtenerHistorial();
        modelo.setRowCount(0);
        int numero = 1;
        for (RegistroEntrega registro : registros) {
            modelo.addRow(new Object[]{
                    numero++,
                    registro.getIdPedido(),
                    registro.getTipoPedido(),
                    registro.getDireccionEntrega(),
                    registro.getNombreRepartidor(),
                    registro.getTiempoEntregaMin(),
                    registro.getEstado(),
                    registro.getFechaHora().format(FORMATO_FECHA_HORA)
            });
        }
        contador.setText(registros.isEmpty()
                ? "Sin entregas registradas en esta sesion"
                : registros.size() + " entrega(s) | Total: "
                        + controladorEnvios.getEntregasRealizadas());
    }
}
