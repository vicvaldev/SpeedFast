package modelos.contratos;

import modelos.implementacion.Pedido;

public interface Cancelable {
    void cancelar(Pedido pedido);
}
