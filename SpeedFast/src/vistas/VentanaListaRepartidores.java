package vistas;

import controladores.ControladorDePedidos;
import modelos.implementacion.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * Gestion de repartidores en una sola pantalla: alta, listado, edicion y
 * eliminacion de la fila seleccionada.
 */
public class VentanaListaRepartidores extends VentanaListado<Repartidor> {

    private final ControladorDePedidos controladorPedidos;
    private final Runnable actualizador;

    public VentanaListaRepartidores(VentanaPrincipal principal, ControladorDePedidos controladorPedidos) {
        super(principal, "Gestión de Repartidores", new String[]{"ID", "Nombre"}, 520, 320);
        this.controladorPedidos = controladorPedidos;
        this.actualizador = this::refrescar;

        JButton btnRegistrar = new JButton("Registrar");
        btnRegistrar.addActionListener(e -> DialogoRepartidor.nuevo(this, controladorPedidos, actualizador));

        JButton btnEditar = new JButton("Editar");
        btnEditar.addActionListener(e -> editar());

        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.addActionListener(e -> eliminar());

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> refrescar());

        JPanel panelBotones = new JPanel();
        panelBotones.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelBotones.add(btnRegistrar);
        panelBotones.add(btnEditar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnRefrescar);
        agregarPanelInferior(panelBotones);

        principal.agregarActualizador(actualizador);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                principal.quitarActualizador(actualizador);
            }
        });

        refrescar();
    }

    @Override
    protected List<Repartidor> consultar() throws SQLException {
        return controladorPedidos.listarRepartidores();
    }

    @Override
    protected Object[] aFila(Repartidor repartidor) {
        return new Object[]{repartidor.getIdRepartidor(), repartidor.getNombre()};
    }

    @Override
    protected String describir(Repartidor repartidor) {
        return "repartidor \"" + repartidor.getNombre() + "\"";
    }

    private void editar() {
        if (!exigirSeleccion()) {
            return;
        }
        Repartidor repartidor = elementoSeleccionado();
        DialogoRepartidor.editar(this, controladorPedidos, repartidor, actualizador);
    }

    /**
     * Confirma el borrado y avisa cuantas entregas se perderan: la FK de la
     * tabla `entrega` las borra en cascada junto con el repartidor.
     */
    private void eliminar() {
        if (!exigirSeleccion()) {
            return;
        }
        Repartidor repartidor = elementoSeleccionado();
        int idRepartidor = repartidor.getIdRepartidor();

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws SQLException {
                return controladorPedidos.cantidadEntregasDeRepartidor(idRepartidor);
            }

            @Override
            protected void done() {
                int entregas;
                try {
                    entregas = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    mostrarErrorDeBase("No se pudo verificar las entregas del repartidor.", e.getCause());
                    return;
                }

                String advertencia = entregas == 0 ? "" : "\n\nSe borrarán también sus " + entregas
                        + " entrega(s) registradas.";
                int opcion = JOptionPane.showConfirmDialog(VentanaListaRepartidores.this,
                        "¿Eliminar el " + describir(repartidor) + "?" + advertencia,
                        "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
                if (opcion != JOptionPane.YES_OPTION) {
                    return;
                }

                ejecutarEscritura("No se pudo eliminar el repartidor.",
                        () -> controladorPedidos.eliminarRepartidor(idRepartidor),
                        () -> {
                            tabla.clearSelection();
                            actualizador.run();
                        });
            }
        }.execute();
    }
}
