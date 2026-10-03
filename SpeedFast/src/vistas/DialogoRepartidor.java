package vistas;

import controladores.ControladorDePedidos;
import modelos.implementacion.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
 * Formulario unico de repartidores: sirve tanto para el alta de uno nuevo como
 * para editar el nombre de uno existente, de modo que la gestion de repartidores
 * se resuelve en una sola pantalla.
 *
 * <p>El unico campo es el nombre; el ID lo asigna la base de datos en el alta y
 * queda congelado en la edicion.</p>
 */
public class DialogoRepartidor extends JDialog {

    private static final String SQL_DUPLICADO = "DUPLICADO";

    private final ControladorDePedidos controladorPedidos;
    private final Repartidor existente;
    private final Runnable alGuardar;
    private final JTextField txtNombre;
    private final JButton btnGuardar;

    private DialogoRepartidor(JFrame propietario, ControladorDePedidos controladorPedidos,
                             Repartidor existente, Runnable alGuardar) {
        super(propietario, existente == null ? "Registrar Repartidor"
                : "Editar Repartidor #" + existente.getIdRepartidor(), true);
        this.controladorPedidos = controladorPedidos;
        this.existente = existente;
        this.alGuardar = alGuardar;

        setSize(400, 160);
        setLocationRelativeTo(propietario);
        setLayout(new BorderLayout());

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;
        gbc.gridx = 0;
        panelFormulario.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        txtNombre = new JTextField(existente == null ? "" : existente.getNombre(), 20);
        panelFormulario.add(txtNombre, gbc);

        add(panelFormulario, BorderLayout.CENTER);

        btnGuardar = new JButton(existente == null ? "Registrar" : "Guardar cambios");
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
     * Abre el formulario en modo alta, con el nombre vacio.
     */
    public static void nuevo(JFrame propietario, ControladorDePedidos controladorPedidos,
                             Runnable alGuardar) {
        new DialogoRepartidor(propietario, controladorPedidos, null, alGuardar).setVisible(true);
    }

    /**
     * Abre el formulario en modo edicion sobre el repartidor seleccionado.
     */
    public static void editar(JFrame propietario, ControladorDePedidos controladorPedidos,
                              Repartidor repartidor, Runnable alGuardar) {
        new DialogoRepartidor(propietario, controladorPedidos, repartidor, alGuardar).setVisible(true);
    }

    private void guardar() {
        final boolean esAlta = existente == null;
        final String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            mostrarError("El nombre del repartidor no puede estar vacío.");
            return;
        }

        // La validacion de duplicados y la escritura viajan a MySQL: se ejecutan
        // en doInBackground para no congelar la ventana, y el resultado se
        // presenta en done() (Event Dispatch Thread).
        btnGuardar.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                // En la edicion el propio repartidor se excluye de la busqueda:
                // conservar su nombre no es una colision. En el alta no hay
                // repartidor propio que ignorar, asi que se excluye el 0.
                int idExcluido = esAlta ? 0 : existente.getIdRepartidor();
                if (controladorPedidos.existeNombreRepartidor(nombre, idExcluido)) {
                    throw new SQLException("Ya existe un repartidor con el nombre " + nombre + ".", SQL_DUPLICADO);
                }
                if (esAlta) {
                    controladorPedidos.crearRepartidor(new Repartidor(0, nombre));
                } else {
                    existente.setNombre(nombre);
                    controladorPedidos.actualizarRepartidor(existente);
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
                                ? "No se pudo registrar el repartidor."
                                : "No se pudo actualizar el repartidor.", causa);
                    }
                    return;
                }

                if (esAlta) {
                    JOptionPane.showMessageDialog(DialogoRepartidor.this,
                            "Repartidor " + nombre + " registrado correctamente.",
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