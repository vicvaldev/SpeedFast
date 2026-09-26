package vistas;

import controladores.ControladorDePedidos;
import modelos.implementacion.ControladorDeEnvios;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public class VentanaPrincipal extends JFrame {

    private final ControladorDePedidos controladorPedidos;
    private final ControladorDeEnvios controladorEnvios;
    private final List<Runnable> actualizadoresDeTabla = new ArrayList<>();

    public VentanaPrincipal(ControladorDePedidos controladorPedidos, ControladorDeEnvios controladorEnvios) {
        this.controladorPedidos = controladorPedidos;
        this.controladorEnvios = controladorEnvios;

        setTitle("Sistema SpeedFast");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 380);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panelTitulo = new JPanel();
        panelTitulo.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        panelTitulo.add(new JLabel("Administración de pedidos SpeedFast"));
        add(panelTitulo, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel(new GridLayout(6, 1, 8, 8));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        JButton btnRegistrar = new JButton("Registrar pedido");
        btnRegistrar.addActionListener(e ->
                new VentanaRegistroPedido(this, controladorPedidos, this::refrescarPedidos).setVisible(true));

        JButton btnRegistrarRepartidor = new JButton("Registrar repartidor");
        btnRegistrarRepartidor.addActionListener(e ->
                new VentanaRegistroRepartidor(this, controladorPedidos, this::refrescarPedidos).setVisible(true));

        JButton btnListar = new JButton("Listar pedidos");
        btnListar.addActionListener(e ->
                new VentanaListaPedidos(this, controladorPedidos).setVisible(true));

        JButton btnHistorial = new JButton("Historial de entregas");
        btnHistorial.addActionListener(e ->
                new VentanaHistorialEntregas(this, controladorEnvios).setVisible(true));

        JButton btnAsignar = new JButton("Asignar repartidor / Iniciar entrega");
        btnAsignar.addActionListener(e -> new DialogoAsignarRepartidor(
                this, controladorPedidos, controladorEnvios, this::refrescarPedidos).setVisible(true));

        JButton btnSalir = new JButton("Salir");
        btnSalir.addActionListener(e -> dispose());

        panelBotones.add(btnRegistrar);
        panelBotones.add(btnRegistrarRepartidor);
        panelBotones.add(btnListar);
        panelBotones.add(btnHistorial);
        panelBotones.add(btnAsignar);
        panelBotones.add(btnSalir);

        add(panelBotones, BorderLayout.CENTER);

        verificarConexionEnSegundoPlano();
    }

    public void agregarActualizador(Runnable actualizador) {
        actualizadoresDeTabla.add(actualizador);
    }

    public void quitarActualizador(Runnable actualizador) {
        actualizadoresDeTabla.remove(actualizador);
    }

    /**
     * Avisa a las vistas abiertas que el conjunto de datos cambio. Cada vista
     * registrada vuelve a consultar los datos por su cuenta.
     */
    public void refrescarPedidos() {
        for (Runnable actualizador : actualizadoresDeTabla) {
            actualizador.run();
        }
    }

    /**
     * Comprueba la disponibilidad de la base de datos fuera del Event Dispatch
     * Thread: abrir una conexion JDBC puede tardar varios segundos y bloquear
     * la interfaz. El aviso se muestra en {@code done()}, que si corre en el EDT.
     */
    private void verificarConexionEnSegundoPlano() {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return controladorPedidos.verificarDisponibilidad();
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    // La verificacion es best effort: un fallo aqui no debe
                    // impedir que la aplicacion se abra.
                    return;
                }
                JOptionPane.showMessageDialog(VentanaPrincipal.this,
                        "No se pudo conectar con la base de datos speedfastdb.\n\n"
                                + "Verifique que el contenedor MySQL esté detenido o revise las "
                                + "credenciales en persistencia/ConexionDB.java.",
                        "Error de conexión", JOptionPane.ERROR_MESSAGE);
            }
        }.execute();
    }
}
