package vistas;

import controladores.ControladorDePedidos;
import modelos.implementacion.Pedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class VentanaListaPedidos extends JFrame {
    private final VentanaPrincipal principal;
    private final ControladorDePedidos controladorPedidos;
    private final Runnable actualizador;
    private final DefaultTableModel modelo;
    private final JTable tabla;

    public VentanaListaPedidos(VentanaPrincipal principal, ControladorDePedidos controladorPedidos) {
        this.principal = principal;
        this.controladorPedidos = controladorPedidos;
        this.actualizador = this::actualizarTabla;

        setTitle("Listado de Pedidos");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(560, 360);
        setLocationRelativeTo(principal);
        setLayout(new BorderLayout());

        modelo = new DefaultTableModel(new String[]{"ID", "Tipo", "Dirección", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> actualizarTabla());

        JPanel panelBotones = new JPanel();
        panelBotones.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelBotones.add(btnRefrescar);
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
        // La consulta JDBC nunca debe ejecutarse en el EDT: si se hiciera aqui,
        // la ventana quedaria congelada mientras MySQL responde.
        new SwingWorker<List<Pedido>, Void>() {
            @Override
            protected List<Pedido> doInBackground() throws SQLException {
                return controladorPedidos.listarTodos();
            }

            @Override
            protected void done() {
                try {
                    modelo.setRowCount(0);
                    for (Pedido pedido : get()) {
                        modelo.addRow(new Object[]{
                                pedido.getIdPedido(),
                                pedido.getTipoPedido(),
                                pedido.getDireccionEntrega(),
                                pedido.getEstado()
                        });
                    }
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    String detalle = causa instanceof SQLException
                            ? causa.getMessage() : String.valueOf(causa);
                    JOptionPane.showMessageDialog(VentanaListaPedidos.this,
                            "No se pudieron consultar los pedidos.\n" + detalle,
                            "Error de base de datos", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
