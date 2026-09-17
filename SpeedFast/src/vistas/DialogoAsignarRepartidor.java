package vistas;

import modelos.implementacion.EstadoPedido;
import modelos.implementacion.Pedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public class DialogoAsignarRepartidor extends JDialog {
    private final List<Pedido> pendientes;
    private final JComboBox<Pedido> cmbPedidos;
    private final JTextField txtRepartidor;

    public DialogoAsignarRepartidor(JFrame propietario, List<Pedido> pedidos) {
        super(propietario, "Asignar Repartidor / Iniciar Entrega", true);

        setSize(440, 180);
        setLocationRelativeTo(propietario);
        setLayout(new BorderLayout());

        pendientes = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == EstadoPedido.PENDIENTE) {
                pendientes.add(pedido);
            }
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        panel.add(new JLabel("Pedido pendiente:"));
        cmbPedidos = new JComboBox<>();
        for (Pedido pedido : pendientes) {
            cmbPedidos.addItem(pedido);
        }
        panel.add(cmbPedidos);

        panel.add(new JLabel("Repartidor:"));
        txtRepartidor = new JTextField();
        panel.add(txtRepartidor);

        add(panel, BorderLayout.CENTER);

        JButton btnAsignar = new JButton("Asignar / Iniciar entrega");
        btnAsignar.addActionListener(e -> asignar());

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());

        JPanel panelBotones = new JPanel();
        panelBotones.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));
        panelBotones.add(btnAsignar);
        panelBotones.add(btnCerrar);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private void asignar() {
        if (pendientes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay pedidos pendientes para asignar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nombre = txtRepartidor.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe ingresar el nombre del repartidor.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Pedido pedido = (Pedido) cmbPedidos.getSelectedItem();
        pedido.asignarRepartidor(nombre);

        JOptionPane.showMessageDialog(this,
                "Pedido #" + pedido.getIdPedido() + " asignado al repartidor " + nombre + ".",
                "Confirmación", JOptionPane.INFORMATION_MESSAGE);

        dispose();
    }
}