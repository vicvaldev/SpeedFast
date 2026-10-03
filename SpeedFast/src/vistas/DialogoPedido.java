package vistas;

import controladores.ControladorDePedidos;
import modelos.implementacion.EstadoPedido;
import modelos.implementacion.Pedido;
import modelos.implementacion.PedidoFabrica;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

/**
 * Formulario unico de pedidos: sirve tanto para el alta de uno nuevo como para
 * editar uno existente, de modo que la gestion de pedidos se resuelve en una
 * sola pantalla.
 *
 * <p>La diferencia entre ambos modos es que en el alta el ID lo elige el usuario
 * y hay que descartar duplicados, mientras que en la edicion el ID es la clave
 * del UPDATE y por eso queda deshabilitado.</p>
 *
 * <p>La subclase concreta la decide {@link PedidoFabrica} a partir del tipo, de
 * modo que el formulario no necesita conocer la jerarquia.</p>
 */
public class DialogoPedido extends JDialog {

    private static final String SQL_DUPLICADO = "DUPLICADO";

    private final ControladorDePedidos controladorPedidos;
    private final Pedido existente;
    private final Runnable alGuardar;
    private final JTextField txtId;
    private final JTextField txtDireccion;
    private final JTextField txtDistancia;
    private final JComboBox<String> cmbTipo;
    private final JComboBox<EstadoPedido> cmbEstado;
    private final JButton btnGuardar;

    private DialogoPedido(JFrame propietario, ControladorDePedidos controladorPedidos,
                          Pedido existente, Runnable alGuardar) {
        super(propietario, existente == null ? "Registrar Pedido"
                : "Editar Pedido #" + existente.getIdPedido(), true);
        this.controladorPedidos = controladorPedidos;
        this.existente = existente;
        this.alGuardar = alGuardar;

        setSize(420, 280);
        setLocationRelativeTo(propietario);
        setLayout(new BorderLayout());

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        boolean esAlta = existente == null;
        txtId = new JTextField(esAlta ? "" : String.valueOf(existente.getIdPedido()), 12);
        if (!esAlta) {
            // El id no se modifica: es la clave con la que la base de datos
            // localiza la fila dentro del UPDATE.
            txtId.setEditable(false);
            txtId.setEnabled(false);
        }
        txtDireccion = new JTextField(esAlta ? "" : existente.getDireccionEntrega(), 20);
        txtDistancia = new JTextField(esAlta ? "0" : String.valueOf(existente.getDistanciaKm()), 12);
        cmbTipo = new JComboBox<>(new String[]{
                PedidoFabrica.TIPO_COMIDA, PedidoFabrica.TIPO_ENCOMIENDA, PedidoFabrica.TIPO_EXPRESS});
        cmbEstado = new JComboBox<>(EstadoPedido.values());
        if (esAlta) {
            cmbTipo.setSelectedIndex(0);
            cmbEstado.setSelectedItem(EstadoPedido.PENDIENTE);
        } else {
            cmbTipo.setSelectedItem(existente.getTipoPedido());
            cmbEstado.setSelectedItem(existente.getEstado());
        }

        agregarCampo(panelFormulario, gbc, 0, "ID:", txtId);
        agregarCampo(panelFormulario, gbc, 1, "Dirección:", txtDireccion);
        agregarCampo(panelFormulario, gbc, 2, "Tipo:", cmbTipo);
        agregarCampo(panelFormulario, gbc, 3, "Estado:", cmbEstado);
        agregarCampo(panelFormulario, gbc, 4, "Distancia (km):", txtDistancia);

        add(panelFormulario, BorderLayout.CENTER);

        btnGuardar = new JButton(esAlta ? "Registrar" : "Guardar cambios");
        btnGuardar.addActionListener(e -> guardar());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        JPanel panelBotones = new JPanel();
        panelBotones.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);
        add(panelBotones, BorderLayout.SOUTH);
    }

    /**
     * Abre el formulario en modo alta, con el ID vacio y disponible.
     */
    public static void nuevo(JFrame propietario, ControladorDePedidos controladorPedidos,
                             Runnable alGuardar) {
        new DialogoPedido(propietario, controladorPedidos, null, alGuardar).setVisible(true);
    }

    /**
     * Abre el formulario en modo edicion sobre el pedido seleccionado.
     */
    public static void editar(JFrame propietario, ControladorDePedidos controladorPedidos,
                              Pedido pedido, Runnable alGuardar) {
        new DialogoPedido(propietario, controladorPedidos, pedido, alGuardar).setVisible(true);
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

    private void guardar() {
        boolean esAlta = existente == null;

        int idPedido;
        if (esAlta) {
            try {
                idPedido = Integer.parseInt(txtId.getText().trim());
                if (idPedido <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                mostrarError("El ID debe ser un número entero positivo.");
                return;
            }
        } else {
            idPedido = existente.getIdPedido();
        }

        String direccion = txtDireccion.getText().trim();
        if (direccion.isEmpty()) {
            mostrarError("La dirección no puede estar vacía.");
            return;
        }

        double distancia;
        try {
            distancia = Double.parseDouble(txtDistancia.getText().trim());
            if (distancia < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            mostrarError("La distancia debe ser un número mayor o igual a 0.");
            return;
        }

        String tipo = (String) cmbTipo.getSelectedItem();
        EstadoPedido estado = (EstadoPedido) cmbEstado.getSelectedItem();

        // Si el tipo no cambia se reutiliza el objeto original para no perder sus
        // datos de detalle; si cambia, la fabrica construye la subclase nueva.
        Pedido pedido;
        if (esAlta) {
            pedido = PedidoFabrica.crear(tipo, idPedido, direccion, distancia);
        } else {
            pedido = tipo.equals(existente.getTipoPedido())
                    ? existente
                    : PedidoFabrica.crear(tipo, idPedido, direccion, distancia);
        }
        pedido.setDireccionEntrega(direccion);
        pedido.setDistanciaKm(distancia);
        pedido.setEstado(estado);

        // La validacion de duplicados y la escritura son viajes de ida y vuelta
        // a MySQL: se ejecutan en doInBackground para no congelar la ventana,
        // y solo la parte visual se resuelve en done() (Event Dispatch Thread).
        btnGuardar.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                if (esAlta && controladorPedidos.existePedido(idPedido)) {
                    throw new SQLException("Ya existe un pedido con el ID " + idPedido + ".", SQL_DUPLICADO);
                }
                if (esAlta) {
                    controladorPedidos.crearPedido(pedido);
                } else {
                    controladorPedidos.actualizarPedido(pedido);
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
                    Throwable causa = e.getCause();
                    if (causa instanceof SQLException
                            && SQL_DUPLICADO.equals(((SQLException) causa).getSQLState())) {
                        mostrarError(causa.getMessage());
                    } else {
                        mostrarErrorDeBase(esAlta
                                ? "No se pudo registrar el pedido."
                                : "No se pudo actualizar el pedido.", causa);
                    }
                    return;
                }

                if (esAlta) {
                    JOptionPane.showMessageDialog(DialogoPedido.this,
                            "Pedido #" + idPedido + " (" + tipo + ") registrado correctamente.",
                            "Confirmación", JOptionPane.INFORMATION_MESSAGE);
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