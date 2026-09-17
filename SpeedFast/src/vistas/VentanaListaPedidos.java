package vistas;

import modelos.implementacion.Pedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;

public class VentanaListaPedidos extends JFrame {
    private final List<Pedido> pedidos;
    private final DefaultTableModel modelo;
    private final JTable tabla;

    public VentanaListaPedidos(List<Pedido> pedidos) {
        this.pedidos = pedidos;

        setTitle("Listado de Pedidos");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(560, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        modelo = new DefaultTableModel(new String[]{"ID", "Tipo", "Dirección", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> actualizarTabla());

        JPanel panelBotones = new JPanel();
        panelBotones.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelBotones.add(btnRefrescar);
        add(panelBotones, BorderLayout.SOUTH);

        actualizarTabla();
    }

    private void actualizarTabla() {
        modelo.setRowCount(0);
        for (Pedido pedido : pedidos) {
            modelo.addRow(new Object[]{
                    pedido.getIdPedido(),
                    pedido.getTipoPedido(),
                    pedido.getDireccionEntrega(),
                    pedido.getEstado()
            });
        }
    }
}