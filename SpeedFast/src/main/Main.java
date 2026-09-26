package main;

import controladores.ControladorDePedidos;
import modelos.implementacion.ControladorDeEnvios;
import vistas.VentanaPrincipal;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        // Composition root: aqui se construyen los controladores y se inyectan
        // en la vista. La ventana no crea ni conoce capas inferiores.
        ControladorDePedidos controladorPedidos = new ControladorDePedidos();
        ControladorDeEnvios controladorEnvios = new ControladorDeEnvios();

        // Swing exige que toda la creacion y manipulacion de la interfaz
        // ocurra en el Event Dispatch Thread; invokeLater agenda esa tarea.
        SwingUtilities.invokeLater(() ->
                new VentanaPrincipal(controladorPedidos, controladorEnvios).setVisible(true));
    }
}
