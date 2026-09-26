package vistas;

import controladores.ControladorDePedidos;
import modelos.implementacion.Pedido;
import modelos.implementacion.PedidoComida;
import modelos.implementacion.PedidoEncomienda;
import modelos.implementacion.PedidoExpress;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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

public class VentanaRegistroPedido extends JFrame {
    private final Runnable alGuardar;
    private final ControladorDePedidos controladorPedidos;
    private final JTextField txtId;
    private final JTextField txtDireccion;
    private final JComboBox<String> cmbTipo;
    private final JButton btnGuardar;

    public VentanaRegistroPedido(VentanaPrincipal principal,
                                 ControladorDePedidos controladorPedidos,
                                 Runnable alGuardar) {
        this.alGuardar = alGuardar;
        this.controladorPedidos = controladorPedidos;

        setTitle("Registrar Pedido");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 220);
        setLocationRelativeTo(principal);
        setLayout(new BorderLayout());

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        txtId = new JTextField(12);
        txtDireccion = new JTextField(20);
        cmbTipo = new JComboBox<>(new String[]{"Comida", "Encomienda", "Express"});

        agregarCampo(panelFormulario, gbc, 0, "ID:", txtId);
        agregarCampo(panelFormulario, gbc, 1, "Dirección:", txtDireccion);
        agregarCampo(panelFormulario, gbc, 2, "Tipo:", cmbTipo);

        add(panelFormulario, BorderLayout.CENTER);

        btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> guardarPedido());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        JPanel panelBotones = new JPanel();
        panelBotones.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);
        add(panelBotones, BorderLayout.SOUTH);
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

    private void guardarPedido() {
        int id;
        try {
            id = Integer.parseInt(txtId.getText().trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            mostrarError("El ID debe ser un número entero positivo.");
            return;
        }

        String direccion = txtDireccion.getText().trim();
        if (direccion.isEmpty()) {
            mostrarError("La dirección no puede estar vacía.");
            return;
        }

        String tipo = (String) cmbTipo.getSelectedItem();
        Pedido pedido;
        switch (tipo) {
            case "Comida":
                pedido = new PedidoComida(id, direccion, 0.0, true);
                break;
            case "Encomienda":
                pedido = new PedidoEncomienda(id, direccion, 0.0, 5.0, true);
                break;
            default:
                pedido = new PedidoExpress(id, direccion, 0.0, 1.0);
                break;
        }

        // La validacion de duplicados y el INSERT son dos viajes de ida y vuelta
        // a MySQL: se ejecutan en doInBackground para no congelar la ventana,
        // y solo la parte visual se resuelve en done() (Event Dispatch Thread).
        btnGuardar.setEnabled(false);
        new SwingWorker<Pedido, Void>() {
            @Override
            protected Pedido doInBackground() throws SQLException {
                if (controladorPedidos.existeId(id)) {
                    throw new SQLException("Ya existe un pedido con el ID " + id + ".", "DUPLICADO");
                }
                controladorPedidos.guardar(pedido);
                return pedido;
            }

            @Override
            protected void done() {
                btnGuardar.setEnabled(true);
                Pedido guardado;
                try {
                    guardado = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    if (causa instanceof SQLException
                            && "DUPLICADO".equals(((SQLException) causa).getSQLState())) {
                        mostrarError(causa.getMessage());
                    } else {
                        mostrarErrorDeBase(causa);
                    }
                    return;
                }

                JOptionPane.showMessageDialog(VentanaRegistroPedido.this,
                        "Pedido #" + guardado.getIdPedido() + " (" + tipo + ") registrado correctamente.",
                        "Confirmación", JOptionPane.INFORMATION_MESSAGE);

                txtId.setText("");
                txtDireccion.setText("");
                cmbTipo.setSelectedIndex(0);
                alGuardar.run();
            }
        }.execute();
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error de validación", JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarErrorDeBase(Throwable e) {
        JOptionPane.showMessageDialog(this,
                "No se pudo registrar el pedido en la base de datos.\n"
                        + (e == null ? "Error desconocido." : e.getMessage()),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }
}
