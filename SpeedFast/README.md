# SpeedFast

Sistema de reparto de pedidos que gestiona de forma integral diferentes tipos de envíos, aplicando conceptos de **Programación Orientada a Objetos**: polimorfismo, abstracción e interfaces.

## Descripción del proyecto

SpeedFast administra pedidos diferenciados por tipo — **Comida**, **Encomienda** y **Express** — cada uno con lógica específica de asignación de repartidor y cálculo del tiempo de entrega. Además ofrece interacciones funcionales como reservar, despachar, cancelar y consultar el historial de entregas.

El sistema está desacoplado mediante interfaces que separan responsabilidades comunes a distintas clases.

## Estructura del sistema

```
SpeedFast/
├── src/
│   ├── aplicacion/
│   │   └── Main.java            (simulación / punto de entrada)
│   └── modelos/
│       ├── Pedido.java          (clase abstracta base)
│       ├── PedidoComida.java    (subclase Comida)
│       ├── PedidoEncomienda.java(subclase Encomienda)
│       ├── PedidoExpress.java   (subclase Express)
│       ├── Despachable.java     (interfaz: despachar)
│       ├── Cancelable.java      (interfaz: cancelar)
│       ├── Rastreable.java      (interfaz: verHistorial)
│       └── ControladorDeEnvios.java (implementa las interfaces y el historial)
```

### Conceptos aplicados

- **Polimorfismo**: jerarquía con clase base `Pedido` y subclases `PedidoComida`, `PedidoEncomienda` y `PedidoExpress`. Método sobrescrito `asignarRepartidor()` en cada subclase y método sobrecargado `asignarRepartidor(String nombre)`.
- **Abstracción**: clase abstracta `Pedido` con atributos, método implementado `mostrarResumen()` y método abstracto `calcularTiempoEntrega()` con lógica personalizada en cada subclase.
- **Interfaces**: `Despachable`, `Cancelable` y `Rastreable`, implementadas por la clase `ControladorDeEnvios`, que también mantiene el historial de entregas en un `ArrayList`.

## Requisitos

- JDK 8 o superior (el proyecto compila con Java 26).

## Cómo compilar y ejecutar

1. Clona o abre el proyecto en tu máquina.
2. Desde la raíz del proyecto (`SpeedFast/`), compila las clases:

   ```bash
   javac -encoding UTF-8 -d out -sourcepath src $(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
   ```

3. Ejecuta el programa:

   ```bash
   java -cp out aplicacion.Main
   ```

### Desde IntelliJ IDEA

1. Abre el proyecto con IntelliJ IDEA.
2. Ejecuta la clase `aplicacion.Main` con el botón **Run** o `Shift+F10`.
