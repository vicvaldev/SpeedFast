package vistas;

import controladores.ControladorDePedidos;
import modelos.implementacion.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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

public class VentanaRegistroRepartidor extends JFrame {
    private final Runnable alGuardar;
    private final ControladorDePedidos controladorPedidos;
    private final JTextField txtNombre;
    private final JButton btnGuardar;

    public VentanaRegistroRepartidor(JFrame propietario,
                                      ControladorDePedidos controladorPedidos,
                                      Runnable alGuardar) {
        this.alGuardar = alGuardar;
        this.controladorPedidos = controladorPedidos;

        setTitle("Registrar Repartidor");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 160);
        setLocationRelativeTo(propietario);
        setLayout(new BorderLayout());

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        txtNombre = new JTextField(20);
        agregarCampo(panelFormulario, gbc, 0, "Nombre:", txtNombre);

        add(panelFormulario, BorderLayout.CENTER);

        btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> guardarRepartidor());

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

    private void guardarRepartidor() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            mostrarError("El nombre del repartidor no puede estar vacío.");
            return;
        }

        // La validacion de duplicados y el INSERT viajan a MySQL: se ejecutan
        // en doInBackground para no congelar la ventana, y el resultado se
        // presenta en done() (Event Dispatch Thread).
        btnGuardar.setEnabled(false);
        new SwingWorker<Repartidor, Void>() {
            @Override
            protected Repartidor doInBackground() throws SQLException {
                if (controladorPedidos.existeNombreRepartidor(nombre)) {
                    throw new SQLException("Ya existe un repartidor con el nombre " + nombre + ".", "DUPLICADO");
                }
                Repartidor repartidor = new Repartidor(0, nombre);
                controladorPedidos.guardarRepartidor(repartidor);
                return repartidor;
            }

            @Override
            protected void done() {
                btnGuardar.setEnabled(true);
                Repartidor repartidor;
                try {
                    repartidor = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    if (causa instanceof SQLException
                            && "DUPLICADO".equals(((SQLException) causa).getSQLState())) {
                        mostrarError(causa.getMessage());
                    } else {
                        JOptionPane.showMessageDialog(VentanaRegistroRepartidor.this,
                                "No se pudo registrar el repartidor en la base de datos.\n"
                                        + (causa == null ? "Error desconocido." : causa.getMessage()),
                                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
                    }
                    return;
                }

                JOptionPane.showMessageDialog(VentanaRegistroRepartidor.this,
                        "Repartidor " + repartidor.getNombre() + " registrado con el ID "
                                + repartidor.getIdRepartidor() + ".",
                        "Confirmación", JOptionPane.INFORMATION_MESSAGE);

                txtNombre.setText("");
                alGuardar.run();
            }
        }.execute();
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
