package vistas;

import controladores.ControladorDePedidos;
import modelos.implementacion.Entrega;
import modelos.implementacion.Pedido;
import modelos.implementacion.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Formulario de entrega, usado tanto para el alta como para la edicion.
 *
 * <p>Las dos operaciones comparten los mismos campos, asi que hay un solo
 * formulario: {@link #registrar} lo abre vacio con la fecha y la hora
 * actuales, y {@link #editar} lo abre con los datos de la fila seleccionada. Lo
 * unico que cambia es si la entrega llega con id (editar) o sin id (crear).</p>
 */
public class DialogoEntrega extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ControladorDePedidos controladorPedidos;
    private final Entrega existente;
    private final Runnable alGuardar;
    private final JComboBox<Pedido> cmbPedido;
    private final JComboBox<Repartidor> cmbRepartidor;
    private final JTextField txtFecha;
    private final JTextField txtHora;
    private final JButton btnGuardar;

    /**
     * Abre el formulario para registrar una entrega nueva.
     */
    public static void registrar(Frame propietario, ControladorDePedidos controladorPedidos,
                                Runnable alGuardar) {
        DialogoEntrega dialogo = new DialogoEntrega(propietario, controladorPedidos, null, alGuardar);
        dialogo.setVisible(true);
    }

    /**
     * Abre el formulario con los datos de una entrega ya registrada.
     */
    public static void editar(Frame propietario, ControladorDePedidos controladorPedidos,
                              Entrega existente, Runnable alGuardar) {
        DialogoEntrega dialogo = new DialogoEntrega(propietario, controladorPedidos, existente, alGuardar);
        dialogo.setVisible(true);
    }

    private DialogoEntrega(Frame propietario, ControladorDePedidos controladorPedidos,
                           Entrega existente, Runnable alGuardar) {
        super(propietario, existente == null ? "Registrar Entrega" : "Editar Entrega", true);
        this.controladorPedidos = controladorPedidos;
        this.existente = existente;
        this.alGuardar = alGuardar;

        setSize(460, 260);
        setLocationRelativeTo(propietario);
        setLayout(new BorderLayout());

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        cmbPedido = new JComboBox<>();
        cmbRepartidor = new JComboBox<>();
        txtFecha = new JTextField(12);
        txtHora = new JTextField(12);

        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now().withSecond(0).withNano(0);
        txtFecha.setText(hoy.format(FORMATO_FECHA));
        txtHora.setText(ahora.format(FORMATO_HORA));

        agregarCampo(panelFormulario, gbc, 0, "Pedido:", cmbPedido);
        agregarCampo(panelFormulario, gbc, 1, "Repartidor:", cmbRepartidor);
        agregarCampo(panelFormulario, gbc, 2, "Fecha (dd/MM/aaaa):", txtFecha);
        agregarCampo(panelFormulario, gbc, 3, "Hora (HH:mm):", txtHora);

        add(panelFormulario, BorderLayout.CENTER);

        btnGuardar = new JButton(existente == null ? "Registrar" : "Guardar cambios");
        btnGuardar.addActionListener(e -> guardar());
        btnGuardar.setEnabled(false);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        JPanel panelBotones = new JPanel();
        panelBotones.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);
        add(panelBotones, BorderLayout.SOUTH);

        cargarOpciones();
    }

    private void agregarCampo(JPanel panel, GridBagConstraints gbc, int fila, String etiqueta, JComponent campo) {
        gbc.gridy = fila;
        gbc.gridx = 0;
        panel.add(new JLabel(etiqueta), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(campo, gbc);
        gbc.fill = GridBagConstraints.NONE;
    }

    /**
     * Resultado de la carga de arranque: los dos combos se llenan con el mismo
     * viaje a la base de datos.
     */
    private static class Opciones {
        private final List<Pedido> pedidos;
        private final List<Repartidor> repartidores;

        Opciones(List<Pedido> pedidos, List<Repartidor> repartidores) {
            this.pedidos = pedidos;
            this.repartidores = repartidores;
        }
    }

    /**
     * Carga pedidos y repartidores, y deja seleccionados los de la entrega que
     * se esta editando.
     */
    private void cargarOpciones() {
        new SwingWorker<Opciones, Void>() {
            @Override
            protected Opciones doInBackground() throws SQLException {
                return new Opciones(controladorPedidos.listarPedidos(),
                        controladorPedidos.listarRepartidores());
            }

            @Override
            protected void done() {
                Opciones opciones;
                try {
                    opciones = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    mostrarErrorDeBase("No se pudieron cargar pedidos y repartidores.", e.getCause());
                    return;
                }

                for (Pedido pedido : opciones.pedidos) {
                    cmbPedido.addItem(pedido);
                }
                for (Repartidor repartidor : opciones.repartidores) {
                    cmbRepartidor.addItem(repartidor);
                }

                if (existente != null) {
                    seleccionarPedido(existente.getIdPedido());
                    seleccionarRepartidor(existente.getIdRepartidor());
                    txtFecha.setText(existente.getFecha().format(FORMATO_FECHA));
                    txtHora.setText(existente.getHora().format(FORMATO_HORA));
                }

                if (cmbPedido.getItemCount() == 0 || cmbRepartidor.getItemCount() == 0) {
                    JOptionPane.showMessageDialog(DialogoEntrega.this,
                            "Debe existir al menos un pedido y un repartidor para registrar una entrega.",
                            "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                btnGuardar.setEnabled(true);
            }
        }.execute();
    }

    private void seleccionarPedido(int idPedido) {
        for (int i = 0; i < cmbPedido.getItemCount(); i++) {
            if (cmbPedido.getItemAt(i).getIdPedido() == idPedido) {
                cmbPedido.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarRepartidor(int idRepartidor) {
        for (int i = 0; i < cmbRepartidor.getItemCount(); i++) {
            if (cmbRepartidor.getItemAt(i).getIdRepartidor() == idRepartidor) {
                cmbRepartidor.setSelectedIndex(i);
                return;
            }
        }
    }

    private void guardar() {
        Pedido pedido = (Pedido) cmbPedido.getSelectedItem();
        Repartidor repartidor = (Repartidor) cmbRepartidor.getSelectedItem();
        if (pedido == null || repartidor == null) {
            mostrarError("Debe seleccionar un pedido y un repartidor.");
            return;
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(txtFecha.getText().trim(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            mostrarError("La fecha no es válida. Use el formato dd/MM/aaaa.");
            return;
        }

        LocalTime hora;
        try {
            hora = LocalTime.parse(txtHora.getText().trim(), FORMATO_HORA);
        } catch (DateTimeParseException e) {
            mostrarError("La hora no es válida. Use el formato HH:mm.");
            return;
        }

        Entrega entrega = existente != null
                ? new Entrega(existente.getId(), pedido.getIdPedido(), repartidor.getIdRepartidor(), fecha, hora)
                : new Entrega(pedido.getIdPedido(), repartidor.getIdRepartidor(), fecha, hora);

        btnGuardar.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                if (existente == null) {
                    controladorPedidos.crearEntrega(entrega);
                } else {
                    controladorPedidos.actualizarEntrega(entrega);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    btnGuardar.setEnabled(true);
                    mostrarErrorDeBase(existente == null
                            ? "No se pudo registrar la entrega."
                            : "No se pudo actualizar la entrega.", e.getCause());
                    return;
                }

                alGuardar.run();
                dispose();
            }
        }.execute();
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error de validación", JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarErrorDeBase(String contexto, Throwable causa) {
        JOptionPane.showMessageDialog(this, contexto + "\n"
                        + (causa == null ? "Error desconocido." : causa.getMessage()),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }
}
