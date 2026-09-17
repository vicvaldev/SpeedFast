# SpeedFast

Sistema de reparto de pedidos que gestiona de forma integral diferentes tipos de envíos, aplicando conceptos de **Programación Orientada a Objetos** (polimorfismo, abstracción e interfaces) y de **Programación Concurrente** (hilos y `ExecutorService`).

## Descripción del proyecto

SpeedFast administra pedidos diferenciados por tipo — **Comida**, **Encomienda** y **Express** — cada uno con lógica específica de asignación de repartidor y cálculo del tiempo de entrega. Además ofrece interacciones funcionales como reservar, despachar, cancelar y consultar el historial de entregas.

La simulación concurrente implementa un patrón **productor-consumidor**: dos **generadores de pedidos** producen y cargan pedidos sobre una **zona de carga compartida** mientras varios **repartidores** (hilos consumidores) los retiran y entregan en paralelo, sincronizando el acceso a la sección crítica.

Además, el sistema cuenta con una **interfaz gráfica (Swing)** que permite registrar pedidos, listarlos y asignar repartidor/iniciar entrega desde ventanas `JFrame`/`JDialog`, compartiendo los datos mediante una lista común administrada por la ventana principal.

El sistema está desacoplado mediante interfaces que separan responsabilidades comunes a distintas clases.

## Estructura del sistema

```
SpeedFast/
├── src/
│   ├── aplicacion/
│   │   └── Main.java                  (simulación concurrente / punto de entrada)
│   ├── main/
│   │   └── Main.java                  (punto de entrada de la interfaz gráfica)
│   ├── vistas/
│   │   ├── VentanaPrincipal.java      (JFrame principal: registrar, listar, asignar)
│   │   ├── VentanaRegistroPedido.java (JFrame: formulario de registro de pedidos)
│   │   ├── VentanaListaPedidos.java   (JFrame: JTable con DefaultTableModel)
│   │   └── DialogoAsignarRepartidor.java (JDialog: asignar repartidor / iniciar entrega)
│   └── modelos/
│       ├── contratos/
│       │   ├── Despachable.java       (interfaz: despachar)
│       │   ├── Cancelable.java        (interfaz: cancelar)
│       │   └── Rastreable.java        (interfaz: verHistorial)
│       └── implementacion/
│           ├── Pedido.java            (clase abstracta base)
│           ├── EstadoPedido.java      (enum: PENDIENTE, EN_REPARTO, ENTREGADO)
│           ├── PedidoComida.java      (subclase Comida)
│           ├── PedidoEncomienda.java  (subclase Encomienda)
│           ├── PedidoExpress.java     (subclase Express)
│           ├── ZonaDeCarga.java       (buffer acotado: synchronized + wait/notifyAll)
│           ├── GeneradorDePedidos.java (productor, implementa Runnable)
│           ├── ControladorDeEnvios.java (historial con ReentrantLock y contador AtomicInteger)
│           └── Repartidor.java        (hilo de reparto/consumidor, implementa Runnable)
```

## Conceptos aplicados

- **Polimorfismo**: jerarquía con clase base `Pedido` y subclases `PedidoComida`, `PedidoEncomienda` y `PedidoExpress`. Método sobrescrito `asignarRepartidor()` en cada subclase y método sobrecargado `asignarRepartidor(String nombre)`.
- **Abstracción**: clase abstracta `Pedido` con atributos (`idPedido`, `direccionEntrega`, `distanciaKm`), método implementado `mostrarResumen()` y método abstracto `calcularTiempoEntrega()` con lógica personalizada en cada subclase.
- **Interfaces**: `Despachable`, `Cancelable` y `Rastreable`, implementadas por la clase `ControladorDeEnvios`, que también mantiene el historial de entregas en un `ArrayList`.
- **Concurrencia y sincronización (productor-consumidor)**: la `ZonaDeCarga` es un buffer acotado bloqueante. Sus métodos `agregarPedido()` (productores) y `retirarPedido()` (consumidores) están declarados como `synchronized` y usan `wait()`/`notifyAll()`: los productores esperan cuando la zona está llena y los consumidores cuando está vacía; `cerrarProduccion()` cuenta productores activos y despierta a los consumidores solo cuando no habrá más pedidos. Dos hilos `GeneradorDePedidos` preparan pedidos en paralelo mientras los hilos `Repartidor` los retiran y entregan, simulando el tiempo con `Thread.sleep()` fuera de la sección crítica. `ControladorDeEnvios` protege su historial compartido con `ReentrantLock` y lleva el contador de entregas con `AtomicInteger`. En `Main`, 2 productores y 3 consumidores se ejecutan con `ExecutorService` (`newFixedThreadPool(5)`) y se espera su finalización con `shutdown()`/`awaitTermination()`.
- **Interfaz gráfica (Swing)**: las ventanas del paquete `vistas` emplean `JFrame`, `JDialog`, `JTable` con `DefaultTableModel` (celdas no editables) y `JOptionPane` para validación y confirmación. `VentanaPrincipal` mantiene una lista común de `Pedido` que comparte con las demás ventanas, de modo que los pedidos registrados se reflejan en el listado y en la asignación de repartidor.

## Interfaz gráfica

La aplicación se inicia desde `main.Main`, que ejecuta `new VentanaPrincipal()`. La ventana principal ofrece tres funcionalidades:

1. **Registrar pedido**: abre `VentanaRegistroPedido`, un formulario con campos ID, Dirección y un `JComboBox` de tipo (Comida, Encomienda, Express). El botón **Guardar** valida los campos (ID numérico positivo y sin duplicados, dirección no vacía), crea la subclase de `Pedido` correspondiente, la agrega a la lista común y confirma con `JOptionPane`.
2. **Listar pedidos**: abre `VentanaListaPedidos`, una tabla `JTable` (ID, Tipo, Dirección, Estado) alimentada por un `DefaultTableModel` que se puede refrescar manualmente.
3. **Asignar repartidor / Iniciar entrega**: abre `DialogoAsignarRepartidor`, que permite elegir un pedido pendiente y un repartidor, e invoca `asignarRepartidor(nombre)`.

## Requisitos

- JDK 8 o superior (el proyecto compila con Java 26).

## Cómo compilar y ejecutar

1. Clona o abre el proyecto en tu máquina.
2. Desde la raíz del proyecto (`SpeedFast/`), compila las clases:

   ```bash
   javac -encoding UTF-8 -d out -sourcepath src $(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
   ```

3. Ejecuta el programa:

   - Simulación de consola:
     ```bash
     java -cp out aplicacion.Main
     ```

   - Interfaz gráfica:
     ```bash
     java -cp out main.Main
     ```

### Desde IntelliJ IDEA

1. Abre el proyecto con IntelliJ IDEA.
2. Ejecuta la clase deseada con el botón **Run** o `Shift+F10`:
   - `aplicacion.Main` para la simulación de consola.
   - `main.Main` para la interfaz gráfica.
