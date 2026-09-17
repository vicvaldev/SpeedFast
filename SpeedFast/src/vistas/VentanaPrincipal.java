package vistas;

import modelos.implementacion.ControladorDeEnvios;
import modelos.implementacion.Pedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public class VentanaPrincipal extends JFrame {
    private final List<Pedido> pedidos = new ArrayList<>();
    private final ControladorDeEnvios controlador = new ControladorDeEnvios();
    private final List<Runnable> actualizadoresDeTabla = new ArrayList<>();

    public void agregarActualizador(Runnable actualizador) {
        actualizadoresDeTabla.add(actualizador);
    }

    public void quitarActualizador(Runnable actualizador) {
        actualizadoresDeTabla.remove(actualizador);
    }

    public void refrescarPedidos() {
        for (Runnable actualizador : actualizadoresDeTabla) {
            actualizador.run();
        }
    }

    public VentanaPrincipal() {
        setTitle("Sistema SpeedFast");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panelTitulo = new JPanel();
        panelTitulo.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        panelTitulo.add(new JLabel("Administración de pedidos SpeedFast"));
        add(panelTitulo, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel(new GridLayout(4, 1, 8, 8));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        JButton btnRegistrar = new JButton("Registrar pedido");
        btnRegistrar.addActionListener(e -> new VentanaRegistroPedido(pedidos).setVisible(true));

        JButton btnListar = new JButton("Listar pedidos");
        btnListar.addActionListener(e -> new VentanaListaPedidos(pedidos, this).setVisible(true));

        JButton btnAsignar = new JButton("Asignar repartidor / Iniciar entrega");
        btnAsignar.addActionListener(e -> new DialogoAsignarRepartidor(this, pedidos, controlador, this::refrescarPedidos).setVisible(true));

        JButton btnSalir = new JButton("Salir");
        btnSalir.addActionListener(e -> dispose());

        panelBotones.add(btnRegistrar);
        panelBotones.add(btnListar);
        panelBotones.add(btnAsignar);
        panelBotones.add(btnSalir);

        add(panelBotones, BorderLayout.CENTER);

        setVisible(true);
    }
}