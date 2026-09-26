package vistas;

import controladores.ControladorDePedidos;
import modelos.implementacion.ControladorDeEnvios;
import modelos.implementacion.EstadoPedido;
import modelos.implementacion.Pedido;
import modelos.implementacion.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;

public class DialogoAsignarRepartidor extends JDialog {
    private final ControladorDePedidos controladorPedidos;
    private final ControladorDeEnvios controladorEnvios;
    private final Runnable alEntregar;
    private final JComboBox<Pedido> cmbPedidos;
    private final JComboBox<Repartidor> cmbRepartidores;
    private final JButton btnAsignar;

    public DialogoAsignarRepartidor(JFrame propietario,
                                    ControladorDePedidos controladorPedidos,
                                    ControladorDeEnvios controladorEnvios,
                                    Runnable alEntregar) {
        super(propietario, "Asignar Repartidor / Iniciar Entrega", true);

        setSize(560, 200);
        setLocationRelativeTo(propietario);
        setLayout(new BorderLayout());
        this.controladorPedidos = controladorPedidos;
        this.controladorEnvios = controladorEnvios;
        this.alEntregar = alEntregar;

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        panel.add(new JLabel("Pedido pendiente:"));
        cmbPedidos = new JComboBox<>();
        panel.add(cmbPedidos);

        panel.add(new JLabel("Repartidor:"));
        cmbRepartidores = new JComboBox<>();
        panel.add(cmbRepartidores);

        add(panel, BorderLayout.CENTER);

        btnAsignar = new JButton("Asignar / Iniciar entrega");
        btnAsignar.addActionListener(e -> asignar());
        btnAsignar.setEnabled(false);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());

        JPanel panelBotones = new JPanel();
        panelBotones.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));
        panelBotones.add(btnAsignar);
        panelBotones.add(btnCerrar);
        add(panelBotones, BorderLayout.SOUTH);

        cargarPendientes();
    }

    /**
     * Resultado de la carga inicial: ambas listas se consultan en el mismo
     * viaje a la base de datos para no abrir dos conexiones separadas.
     */
    private static class DatosCarga {
        private final List<Pedido> pedidos;
        private final List<Repartidor> repartidores;

        DatosCarga(List<Pedido> pedidos, List<Repartidor> repartidores) {
            this.pedidos = pedidos;
            this.repartidores = repartidores;
        }
    }

    private void cargarPendientes() {
        new SwingWorker<DatosCarga, Void>() {
            @Override
            protected DatosCarga doInBackground() throws SQLException {
                // Las dos consultas JDBC van en doInBackground: hacerlas en
                // done() congelaria la ventana mientras MySQL responde.
                return new DatosCarga(controladorPedidos.listarPendientes(),
                        controladorPedidos.listarRepartidores());
            }

            @Override
            protected void done() {
                try {
                    DatosCarga datos = get();
                    for (Pedido pedido : datos.pedidos) {
                        cmbPedidos.addItem(pedido);
                    }
                    for (Repartidor repartidor : datos.repartidores) {
                        cmbRepartidores.addItem(repartidor);
                    }
                    boolean hayDatos = cmbPedidos.getItemCount() > 0
                            && cmbRepartidores.getItemCount() > 0;
                    btnAsignar.setEnabled(hayDatos);
                    if (cmbPedidos.getItemCount() == 0) {
                        mostrarAviso("No hay pedidos pendientes para asignar.");
                    } else if (cmbRepartidores.getItemCount() == 0) {
                        mostrarAviso("No hay repartidores registrados. "
                                + "Use \"Registrar repartidor\" en la ventana principal.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    mostrarError("No se pudieron cargar los datos.\n" + obtenerDetalle(e.getCause()));
                }
            }
        }.execute();
    }

    private void asignar() {
        Pedido pedido = (Pedido) cmbPedidos.getSelectedItem();
        Repartidor repartidor = (Repartidor) cmbRepartidores.getSelectedItem();
        if (pedido == null || repartidor == null) {
            mostrarAviso("Debe seleccionar un pedido y un repartidor.");
            return;
        }

        String nombre = repartidor.getNombre();
        pedido.asignarRepartidor(nombre);

        if (pedido.getEstado() != EstadoPedido.EN_REPARTO) {
            JOptionPane.showMessageDialog(this,
                    "El pedido #" + pedido.getIdPedido() + " no cumple las condiciones "
                            + "y no pudo iniciar la entrega. Estado: " + pedido.getEstado() + ".",
                    "Asignación rechazada", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Pedido #" + pedido.getIdPedido() + " asignado a " + nombre
                        + ". Entrega iniciada (estado: EN_REPARTO).",
                "Confirmación", JOptionPane.INFORMATION_MESSAGE);

        btnAsignar.setEnabled(false);
        simularEntrega(pedido, repartidor.getIdRepartidor(), nombre);
    }

    private void simularEntrega(Pedido pedido, int idRepartidor, String nombreRepartidor) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Todo el acceso a la base de datos ocurre aqui, fuera del
                // Event Dispatch Thread, junto con la simulacion del tiempo
                // de entrega.
                controladorPedidos.actualizarEstado(pedido.getIdPedido(), EstadoPedido.EN_REPARTO);
                Thread.sleep(pedido.calcularPausaEntregaMs());
                controladorPedidos.actualizarEstado(pedido.getIdPedido(), EstadoPedido.ENTREGADO);
                controladorPedidos.registrarEntregaEnBD(pedido.getIdPedido(), idRepartidor);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    mostrarError("Ocurrió un problema al registrar la entrega.\n"
                            + obtenerDetalle(e.getCause()));
                    return;
                }
                pedido.setEstado(EstadoPedido.ENTREGADO);
                controladorEnvios.registrarEntrega(pedido, nombreRepartidor);
                alEntregar.run();
                dispose();
            }
        }.execute();
    }

    private String obtenerDetalle(Throwable e) {
        if (e == null) {
            return "Error desconocido.";
        }
        return e instanceof SQLException ? e.getMessage() : e.toString();
    }

    private void mostrarAviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
