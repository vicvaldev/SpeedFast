package vistas;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Base de las ventanas de listado con acciones CRUD.
 *
 * <p>Concentra lo que las tres tablas comparten: el modelo no editable, la
 * carga de datos en segundo plano y el volcado de cada elemento a la tabla.
 * Las subclases aportan que consulta hacen (siempre a traves del
 * {@code ControladorDePedidos}, nunca al SQL), como se convierte un elemento
 * en fila ({@link #aFila(Object)}) y que acciones hay sobre la fila
 * seleccionada.</p>
 *
 * <p>Ninguna operacion JDBC se ejecuta aqui en el Event Dispatch Thread: tanto
 * las lecturas ({@link #refrescar()}) como las escrituras
 * ({@link #ejecutarEscritura}) viajan en un {@link SwingWorker}.</p>
 *
 * @param <T> tipo de entidad listada
 */
abstract class VentanaListado<T> extends JFrame {

    private final DefaultTableModel modelo;
    protected final JTable tabla;

    // Filas de la ultima consultaSuccessful: el indice de la tabla seleccionada
    // se traduce aqui para obtener la entidad completa, sin releer de la BD.
    private List<T> filas = Collections.emptyList();

    protected VentanaListado(JFrame propietario, String titulo, String[] columnas, int ancho, int alto) {
        setTitle(titulo);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(ancho, alto);
        setLocationRelativeTo(propietario);
        setLayout(new BorderLayout());

        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /**
     * Coloca el panel de botones o filtros en la parte inferior.
     */
    protected void agregarPanelInferior(JComponent panel) {
        add(panel, BorderLayout.SOUTH);
    }

    /**
     * Consulta que llena la tabla. Se ejecuta fuera del Event Dispatch Thread.
     */
    protected abstract List<T> consultar() throws SQLException;

    /**
     * Convierte un elemento en la fila que se muestra, en el mismo orden de las
     * columnas declaradas en el constructor.
     */
    protected abstract Object[] aFila(T elemento);

    /**
     * Nombre de la entidad para los mensajes, por ejemplo "pedido #12".
     */
    protected abstract String describir(T elemento);

    /**
     * Vuelve a consultar y repinta la tabla.
     */
    protected void refrescar() {
        new SwingWorker<List<T>, Void>() {
            @Override
            protected List<T> doInBackground() throws SQLException {
                return consultar();
            }

            @Override
            protected void done() {
                try {
                    pintar(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    mostrarErrorDeBase("No se pudo consultar la información.", e.getCause());
                }
            }
        }.execute();
    }

    private void pintar(List<T> elementos) {
        filas = elementos;
        modelo.setRowCount(0);
        for (T elemento : elementos) {
            modelo.addRow(aFila(elemento));
        }
    }

    /**
     * @return la entidad de la fila seleccionada, o {@code null} si no hay
     * ninguna seleccionada
     */
    protected T elementoSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= filas.size()) {
            return null;
        }
        return filas.get(fila);
    }

    /**
     * Exige que haya una fila seleccionada antes de ejecutar una accion.
     *
     * @return {@code true} si la hay
     */
    protected boolean exigirSeleccion() {
        if (elementoSeleccionado() == null) {
            JOptionPane.showMessageDialog(this,
                    "Debe seleccionar una fila de la tabla.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Escritura sobre la base de datos fuera del Event Dispatch Thread.
     */
    @FunctionalInterface
    protected interface OperacionJdbc {
        void ejecutar() throws SQLException;
    }

    /**
     * Ejecuta una escritura en segundo plano y, si termina bien, invoca
     * {@code alTerminar} para que la ventana se repinte.
     *
     * @param contextoError frase que se muestra si la escritura falla
     */
    protected void ejecutarEscritura(String contextoError, OperacionJdbc operacion, Runnable alTerminar) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                operacion.ejecutar();
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
                    mostrarErrorDeBase(contextoError, e.getCause());
                    return;
                }
                alTerminar.run();
            }
        }.execute();
    }

    /**
     * Muestra el error de una operacion de persistencia, sin perder el detalle
     * que entrega el driver.
     */
    protected void mostrarErrorDeBase(String contexto, Throwable causa) {
        String detalle = causa == null
                ? "Error desconocido."
                : (causa instanceof SQLException ? causa.getMessage() : causa.toString());
        JOptionPane.showMessageDialog(this, contexto + "\n" + detalle,
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }
}
