package vistas;

import controladores.ControladorDePedidos;
import modelos.implementacion.DetalleEntrega;
import modelos.implementacion.Pedido;
import modelos.implementacion.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gestion de entregas: listado con filtros por pedido o por repartidor, mas
 * alta, edicion y eliminacion de la fila seleccionada.
 *
 * <p>Las filas llegan desde un JOIN hecho en el DAO, de modo que la tabla
 * muestra el tipo y la direccion del pedido y el nombre del repartidor sin que
 * esta ventana tenga que consultar nada por su cuenta.</p>
 */
public class VentanaListaEntregas extends VentanaListado<DetalleEntrega> {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static final String CRITERIO_TODAS = "Todas las entregas";
    private static final String CRITERIO_PEDIDO = "Por pedido";
    private static final String CRITERIO_REPARTIDOR = "Por repartidor";

    private final ControladorDePedidos controladorPedidos;
    private final Runnable actualizador;
    private final JComboBox<String> cmbCriterio;
    private final JComboBox<Object> cmbSeleccion;

    public VentanaListaEntregas(VentanaPrincipal principal, ControladorDePedidos controladorPedidos) {
        super(principal, "Gestión de Entregas",
                new String[]{"ID", "Pedido", "Tipo", "Dirección", "Repartidor", "Fecha", "Hora"}, 860, 380);
        this.controladorPedidos = controladorPedidos;
        this.actualizador = this::refrescar;

        cmbCriterio = new JComboBox<>(new String[]{CRITERIO_TODAS, CRITERIO_PEDIDO, CRITERIO_REPARTIDOR});
        cmbCriterio.addActionListener(e -> cargarOpcionesFiltro());
        cmbSeleccion = new JComboBox<>();

        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panelFiltros.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        panelFiltros.add(new JLabel("Mostrar:"));
        panelFiltros.add(cmbCriterio);
        panelFiltros.add(cmbSeleccion);
        add(panelFiltros, BorderLayout.NORTH);

        JButton btnRegistrar = new JButton("Registrar entrega");
        btnRegistrar.addActionListener(e ->
                DialogoEntrega.registrar(this, controladorPedidos, actualizador));

        JButton btnEditar = new JButton("Editar");
        btnEditar.addActionListener(e -> editar());

        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.addActionListener(e -> eliminar());

        JButton btnFiltrar = new JButton("Aplicar filtro");
        btnFiltrar.addActionListener(e -> refrescar());

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> refrescar());

        JPanel panelBotones = new JPanel();
        panelBotones.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelBotones.add(btnRegistrar);
        panelBotones.add(btnEditar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnFiltrar);
        panelBotones.add(btnRefrescar);
        agregarPanelInferior(panelBotones);

        principal.agregarActualizador(actualizador);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                principal.quitarActualizador(actualizador);
            }
        });

        cargarOpcionesFiltro();
        refrescar();
    }

    /**
     * Llena el segundo combo segun el criterio elegido: pedidos o repartidores.
     * Es una consulta a la base de datos, asi que tambien va en segundo plano.
     */
    private void cargarOpcionesFiltro() {
        String criterio = (String) cmbCriterio.getSelectedItem();
        boolean porPedido = CRITERIO_PEDIDO.equals(criterio);
        if (CRITERIO_TODAS.equals(criterio)) {
            cmbSeleccion.setEnabled(false);
            return;
        }
        cmbSeleccion.setEnabled(true);

        new SwingWorker<List<?>, Void>() {
            @Override
            protected List<?> doInBackground() throws SQLException {
                return porPedido
                        ? controladorPedidos.listarPedidos()
                        : controladorPedidos.listarRepartidores();
            }

            @Override
            protected void done() {
                try {
                    cmbSeleccion.removeAllItems();
                    for (Object opcion : get()) {
                        cmbSeleccion.addItem(opcion);
                    }
                    if (cmbSeleccion.getItemCount() == 0) {
                        JOptionPane.showMessageDialog(VentanaListaEntregas.this,
                                porPedido
                                        ? "No hay pedidos registrados todavía."
                                        : "No hay repartidores registrados todavía.",
                                "Aviso", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    mostrarErrorDeBase("No se pudo cargar las opciones del filtro.", e.getCause());
                }
            }
        }.execute();
    }

    @Override
    protected List<DetalleEntrega> consultar() throws SQLException {
        String criterio = (String) cmbCriterio.getSelectedItem();
        Object seleccion = cmbSeleccion.getSelectedItem();

        if (CRITERIO_PEDIDO.equals(criterio) && seleccion instanceof Pedido) {
            return controladorPedidos.listarEntregasDePedido(((Pedido) seleccion).getIdPedido());
        }
        if (CRITERIO_REPARTIDOR.equals(criterio) && seleccion instanceof Repartidor) {
            return controladorPedidos.listarEntregasDeRepartidor(((Repartidor) seleccion).getIdRepartidor());
        }
        return controladorPedidos.listarEntregas();
    }

    @Override
    protected Object[] aFila(DetalleEntrega entrega) {
        return new Object[]{
                entrega.getIdEntrega(),
                entrega.getIdPedido(),
                entrega.getTipoPedido(),
                entrega.getDireccionEntrega(),
                entrega.getNombreRepartidor(),
                entrega.getFecha().format(FORMATO_FECHA),
                entrega.getHora().format(FORMATO_HORA)
        };
    }

    @Override
    protected String describir(DetalleEntrega entrega) {
        return "entrega #" + entrega.getIdEntrega() + " del pedido #" + entrega.getIdPedido();
    }

    private void editar() {
        if (!exigirSeleccion()) {
            return;
        }
        DetalleEntrega entrega = elementoSeleccionado();
        DialogoEntrega.editar(this, controladorPedidos, entrega.toEntrega(), actualizador);
    }

    private void eliminar() {
        if (!exigirSeleccion()) {
            return;
        }
        DetalleEntrega entrega = elementoSeleccionado();
        int opcion = JOptionPane.showConfirmDialog(VentanaListaEntregas.this,
                "¿Eliminar la " + describir(entrega) + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        int idEntrega = entrega.getIdEntrega();
        ejecutarEscritura("No se pudo eliminar la entrega.",
                () -> controladorPedidos.eliminarEntrega(idEntrega),
                () -> {
                    tabla.clearSelection();
                    actualizador.run();
                });
    }
}
