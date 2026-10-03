package vistas;

import controladores.ControladorDePedidos;
import modelos.implementacion.EstadoPedido;
import modelos.implementacion.Pedido;
import modelos.implementacion.PedidoFabrica;

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
import java.util.List;

/**
 * Gestion de pedidos en una sola pantalla: alta, listado con filtros opcionales
 * por estado y por tipo, edicion y eliminacion de la fila seleccionada.
 */
public class VentanaListaPedidos extends VentanaListado<Pedido> {

    private static final String TODOS = "Todos";

    private final ControladorDePedidos controladorPedidos;
    private final Runnable actualizador;
    private final JComboBox<String> cmbTipo;
    private final JComboBox<String> cmbEstado;

    public VentanaListaPedidos(VentanaPrincipal principal, ControladorDePedidos controladorPedidos) {
        super(principal, "Gestión de Pedidos",
                new String[]{"ID", "Tipo", "Dirección", "Estado", "Distancia (km)"}, 720, 380);
        this.controladorPedidos = controladorPedidos;
        this.actualizador = this::refrescar;

        cmbTipo = new JComboBox<>(new String[]{
                TODOS, PedidoFabrica.TIPO_COMIDA, PedidoFabrica.TIPO_ENCOMIENDA, PedidoFabrica.TIPO_EXPRESS});
        cmbEstado = new JComboBox<>(new String[]{
                TODOS, EstadoPedido.PENDIENTE.name(), EstadoPedido.EN_REPARTO.name(),
                EstadoPedido.ENTREGADO.name()});

        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panelFiltros.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        panelFiltros.add(new JLabel("Tipo:"));
        panelFiltros.add(cmbTipo);
        panelFiltros.add(new JLabel("Estado:"));
        panelFiltros.add(cmbEstado);
        add(panelFiltros, BorderLayout.NORTH);

        // Filtrar y refrescar consultan exactamente lo mismo: la consulta ya
        // aplica los valores de los dos combos, por lo que refrescar tras una
        // edicion mantiene el filtro activo.
        JButton btnFiltrar = new JButton("Filtrar");
        btnFiltrar.addActionListener(e -> refrescar());

        JButton btnLimpiar = new JButton("Quitar filtro");
        btnLimpiar.addActionListener(e -> {
            cmbTipo.setSelectedIndex(0);
            cmbEstado.setSelectedIndex(0);
            refrescar();
        });

        JButton btnRegistrar = new JButton("Registrar");
        btnRegistrar.addActionListener(e -> DialogoPedido.nuevo(this, controladorPedidos, actualizador));

        JButton btnEditar = new JButton("Editar");
        btnEditar.addActionListener(e -> editar());

        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.addActionListener(e -> eliminar());

        JPanel panelBotones = new JPanel();
        panelBotones.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelBotones.add(btnRegistrar);
        panelBotones.add(btnFiltrar);
        panelBotones.add(btnLimpiar);
        panelBotones.add(btnEditar);
        panelBotones.add(btnEliminar);
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
    protected List<Pedido> consultar() throws SQLException {
        String tipo = cmbTipo.getSelectedIndex() == 0 ? null : (String) cmbTipo.getSelectedItem();
        String estadoTexto = cmbEstado.getSelectedIndex() == 0 ? null : (String) cmbEstado.getSelectedItem();
        EstadoPedido estado = estadoTexto == null ? null : EstadoPedido.valueOf(estadoTexto);
        return controladorPedidos.listarPedidosFiltrados(tipo, estado);
    }

    @Override
    protected Object[] aFila(Pedido pedido) {
        return new Object[]{
                pedido.getIdPedido(),
                pedido.getTipoPedido(),
                pedido.getDireccionEntrega(),
                pedido.getEstado(),
                pedido.getDistanciaKm()
        };
    }

    @Override
    protected String describir(Pedido pedido) {
        return "pedido #" + pedido.getIdPedido();
    }

    private void editar() {
        if (!exigirSeleccion()) {
            return;
        }
        Pedido pedido = elementoSeleccionado();
        DialogoPedido.editar(this, controladorPedidos, pedido, actualizador);
    }

    /**
     * Confirma el borrado y avisa cuantas entregas se perderan: la FK de la
     * tabla `entrega` las borra en cascada junto con el pedido.
     */
    private void eliminar() {
        if (!exigirSeleccion()) {
            return;
        }
        Pedido pedido = elementoSeleccionado();
        int idPedido = pedido.getIdPedido();

        // El conteo es una consulta mas: va en segundo plano antes de preguntar.
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws SQLException {
                return controladorPedidos.cantidadEntregasDePedido(idPedido);
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
                    mostrarErrorDeBase("No se pudo verificar las entregas del pedido.", e.getCause());
                    return;
                }

                String advertencia = entregas == 0 ? "" : "\n\nSe borrarán también sus " + entregas
                        + " entrega(s) registradas.";
                int opcion = JOptionPane.showConfirmDialog(VentanaListaPedidos.this,
                        "¿Eliminar el " + describir(pedido) + "?" + advertencia,
                        "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
                if (opcion != JOptionPane.YES_OPTION) {
                    return;
                }

                ejecutarEscritura("No se pudo eliminar el pedido.",
                        () -> controladorPedidos.eliminarPedido(idPedido),
                        () -> {
                            tabla.clearSelection();
                            actualizador.run();
                        });
            }
        }.execute();
    }
}
