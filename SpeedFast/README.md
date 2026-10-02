# SpeedFast

Sistema de reparto de pedidos que gestiona de forma integral diferentes tipos de envíos, aplicando conceptos de **Programación Orientada a Objetos** (polimorfismo, abstracción e interfaces), de **Programación Concurrente** (`synchronized` con `wait`/`notifyAll`, `ReentrantLock`, `AtomicInteger` y `SwingWorker`) y de **persistencia en base de datos** (JDBC con el patrón DAO).

## Descripción del proyecto

SpeedFast administra pedidos diferenciados por tipo — **Comida**, **Encomienda** y **Express** — cada uno con lógica específica de asignación de repartidor y cálculo del tiempo de entrega. Además ofrece interacciones funcionales como reservar, despachar, cancelar y consultar el historial de entregas, que se muestra en una tabla dentro de la propia interfaz.

El paquete de modelos incluye además una simulación concurrente con patrón **productor-consumidor**: dos **generadores de pedidos** cargan pedidos sobre una **zona de carga compartida** acotada mientras varios **repartidores** (consumidores) los retiran y entregan en paralelo, sincronizando el acceso a la sección crítica. Esa simulación está disponible como biblioteca (`ZonaDeCarga`, `GeneradorDePedidos`, `Repartidor.run()`), pero **no está conectada al flujo de la interfaz**, que ejecuta la entrega mediante un `SwingWorker`.

La **interfaz gráfica (Swing)** permite registrar pedidos y repartidores, listar los pedidos almacenados, asignar repartidor/iniciar entrega y consultar el historial de entregas. Los datos viven en **MySQL**: el paquete `persistencia` encapsula conexiones y operaciones, y el paquete `controladores` es el único puente hacia ellos, de modo que las vistas nunca construyen conexiones ni conocen DAOs por su cuenta.

El sistema está desacoplado mediante interfaces que separan responsabilidades comunes a distintas clases.

## Estructura del sistema

```
SpeedFast/
├── lib/
│   └── mysql-connector-j-26.7.0.jar  (driver JDBC, no versionado)
├── sql/
│   ├── esquema.sql               (crea la base, las tablas y sus relaciones)
│   └── datos_iniciales.sql       (repartidores de ejemplo, idempotente)
├── src/
│   ├── main/
│   │   └── Main.java                  (composition root: construye los controladores y abre la GUI en el EDT)
│   ├── controladores/
│   │   └── ControladorDePedidos.java  (fachada de pedidos, repartidores y entregas; la inyectan las vistas)
│   ├── persistencia/
│   │   ├── ConexionDB.java            (conexión a MySQL y carga del driver)
│   │   ├── Entrega.java               (DTO de la relación pedido-repartidor)
│   │   ├── PedidoDAO.java             (guardar, listarTodos, listarPendientes, actualizarEstado, eliminar)
│   │   ├── RepartidorDAO.java         (guardar, listarTodos, existeNombre)
│   │   └── EntregaDAO.java            (guardar, listarTodas, listarPorPedido)
│   ├── vistas/
│   │   ├── VentanaPrincipal.java          (JFrame principal: registrar, listar, historial, asignar)
│   │   ├── VentanaRegistroPedido.java     (JFrame: formulario de registro de pedidos)
│   │   ├── VentanaRegistroRepartidor.java (JFrame: formulario de registro de repartidores)
│   │   ├── VentanaListaPedidos.java       (JFrame: JTable con DefaultTableModel)
│   │   ├── VentanaHistorialEntregas.java  (JFrame: JTable del historial en memoria)
│   │   └── DialogoAsignarRepartidor.java  (JDialog: asignar repartidor / iniciar entrega)
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
│           ├── RegistroEntrega.java   (DTO inmutable de una entrada del historial)
│           ├── ControladorDeEnvios.java (historial con ReentrantLock y contador AtomicInteger)
│           └── Repartidor.java        (hilo de reparto/consumidor + hidratación desde la BD)
```

## Conceptos aplicados

- **Polimorfismo**: jerarquía con clase base `Pedido` y subclases `PedidoComida`, `PedidoEncomienda` y `PedidoExpress`. Método sobrescrito `asignarRepartidor()` en cada subclase y método sobrecargado `asignarRepartidor(String nombre)`.
- **Abstracción**: clase abstracta `Pedido` con atributos (`idPedido`, `direccionEntrega`, `distanciaKm`), método implementado `mostrarResumen()` y método abstracto `calcularTiempoEntrega()` con lógica personalizada en cada subclase.
- **Interfaces**: `Despachable`, `Cancelable` y `Rastreable`, implementadas por la clase `ControladorDeEnvios`, que también mantiene el historial de entregas en un `ArrayList` de `RegistroEntrega`.
- **Arquitectura por capas e inyección de dependencias**: `Main` actúa como *composition root* y construye `ControladorDePedidos` y `ControladorDeEnvios`, que se inyectan por constructor en `VentanaPrincipal` y, desde ahí, en cada ventana hija. Ninguna vista importa el paquete `persistencia`: la colección de pedidos ya no vive en `VentanaPrincipal` sino detrás de `ControladorDePedidos`, que es el único punto que conoce los DAO. Cambiar el mecanismo de almacenamiento solo obliga a modificar ese controlador.
- **Concurrencia y sincronización (productor-consumidor)**: la `ZonaDeCarga` es un buffer acotado bloqueante. Sus métodos `agregarPedido()` (productores) y `retirarPedido()` (consumidores) están declarados como `synchronized` y usan `wait()`/`notifyAll()`: los productores esperan cuando la zona está llena y los consumidores cuando está vacía; `cerrarProduccion()` cuenta productores activos y despierta a los consumidores solo cuando no habrá más pedidos. Dos instancias de `GeneradorDePedidos` preparan pedidos en paralelo mientras las instancias de `Repartidor` los retiran y entregan, simulando el tiempo con `Thread.sleep()` fuera de la sección crítica. `ControladorDeEnvios` protege su historial compartido con `ReentrantLock` y lleva el contador de entregas con `AtomicInteger`. El alcance de esta simulación es el del paquete de modelos: la interfaz no la invoca, y su propio reparto se resuelve con `SwingWorker` (ver *Thread safety en la GUI*).
- **Interfaz gráfica (Swing)**: las ventanas del paquete `vistas` emplean `JFrame`, `JDialog`, `JTable` con `DefaultTableModel` (celdas no editables) y `JOptionPane` para validación y confirmación. `VentanaPrincipal` conserva el patrón observador (`agregarActualizador`/`quitarActualizador`) para que las tablas abiertas se actualicen solas.
- **Persistencia (patrón DAO)**: el paquete `persistencia` concentra todo el acceso a datos. `ConexionDB` es el único punto que conoce URL y credenciales; los DAO usan `PreparedStatement` y `ResultSet` siempre dentro de **try-with-resources**, de modo que la conexión y el statement se cierren aunque la operación falle. `EntregaDAO.guardar()` y `RepartidorDAO.guardar()` recuperan la clave generada con `Statement.RETURN_GENERATED_KEYS`.
- **Transacciones**: cuando una acción de negocio toca más de una tabla, el controlador agrupa las escrituras en una única transacción en lugar de delegarlas en DAO independientes. `ControladorDePedidos.registrarEntregaCompleta()` abre una conexión, desactiva el autocommit y ejecuta el `UPDATE pedido SET estado = 'ENTREGADO'` junto con el `INSERT` en `entrega` mediante las sobrecargas `PedidoDAO.actualizarEstado(Connection, …)` y `EntregaDAO.guardar(Connection, …)`, que reciben la conexión del llamador en vez de abrir la suya. Solo si ambas terminan bien se confirma el `commit()`; ante cualquier fallo se hace `rollback()` y la excepción original se propaga hacia la vista. Así el pedido nunca queda marcado como entregado sin su entrega asociada, ni al revés.
- **Mapeo objeto-relacional con polimorfismo**: la tabla `pedido` usa una columna discriminadora (`tipo`) más una columna nullable por atributo propio de cada subclase (`mochila_termica`, `peso`, `embalaje_validado`, `distancia_repartidor_cercano`). `PedidoDAO.crearPedido(ResultSet)` reconstruye la subclase correcta al leer, de modo que un pedido sobrevive intacto a un ciclo guardar → leer. Como esas columnas son nullable, un `NULL` se interpreta con el mismo valor que aplica el formulario, para que el round-trip no termine rechazando la entrega en `validarEntrega()`.
- **Thread safety en la GUI**: **ninguna operación JDBC corre en el Event Dispatch Thread**. `Main` agenda la creación de la ventana con `SwingUtilities.invokeLater()`, y cada consulta o escritura se ejecuta dentro de un `SwingWorker` (`doInBackground`), pintando el resultado solo en `done()`. Los `JOptionPane` de confirmación y error también se invocan desde `done()`.

## Interfaz gráfica

La aplicación se inicia desde `main.Main`, que construye los controladores y agenda la apertura de `VentanaPrincipal` con `SwingUtilities.invokeLater()`, de modo que la interfaz se crea explícitamente en el Event Dispatch Thread. La comprobación de la base de datos corre en un `SwingWorker`: si MySQL no responde, el motivo se muestra en un `JOptionPane` en vez de dejar ventanas vacías sin explicación. La ventana principal ofrece cinco funcionalidades:

1. **Registrar pedido**: abre `VentanaRegistroPedido`, un formulario con campos ID, Dirección y un `JComboBox` de tipo (Comida, Encomienda, Express). El botón **Guardar** valida los campos en el EDT (ID numérico positivo, dirección no vacía) y luego delega la validación de duplicados y el `INSERT` a un `SwingWorker`, que llama a `ControladorDePedidos.existeId()` y `guardar()`. El botón se deshabilita mientras la operación corre y se rehabilita en `done()`, donde se muestra la confirmación o el error. Al guardar, el formulario se limpia y se notifica a las tablas abiertas.
2. **Registrar repartidor**: abre `VentanaRegistroRepartidor`, que valida que el nombre no esté vacío y delega la comprobación de repetidos (`existeNombreRepartidor()`) y el `INSERT` (`guardarRepartidor()`) al mismo patrón de `SwingWorker`.
3. **Listar pedidos**: abre `VentanaListaPedidos`, una tabla `JTable` (ID, Tipo, Dirección, Estado) alimentada por `ControladorDePedidos.listarTodos()` dentro de un `SwingWorker`. También se refresca sola cuando otra ventana notifica un cambio.
4. **Historial de entregas**: abre `VentanaHistorialEntregas`, una `JTable` de solo lectura (#, Pedido, Tipo, Dirección, Repartidor, Tiempo, Estado, Fecha y hora) alimentada por `ControladorDeEnvios.obtenerHistorial()`. A diferencia del resto de ventanas, **no consulta la base de datos**: lee una copia inmodificable del historial en memoria, por lo que puede pintarse directamente en el EDT. Cada entrada es un `RegistroEntrega`, un DTO inmutable que captura el nombre del repartidor y el instante de la entrega en el momento de registrarla. Se refresca sola cuando `DialogoAsignarRepartidor` completa una entrega.
5. **Asignar repartidor / Iniciar entrega**: abre `DialogoAsignarRepartidor`, que carga los pedidos pendientes (`listarPendientes()`) y los repartidores (`listarRepartidores()`) en un único `SwingWorker` de arranque. Se elige un pedido y un repartidor, y se invoca `asignarRepartidor(nombre)`. El estado del pedido cambia según las validaciones existentes (`validarEntrega()`): si supera las condiciones (mochila térmica en Comida, peso ≤ 20 kg y embalaje validado en Encomienda, siempre válido en Express), la asignación se acepta; en caso contrario el pedido permanece `PENDIENTE` y se muestra **Asignación rechazada**. Tras la aceptación, la entrega se simula en segundo plano con un `SwingWorker` que hace todo el acceso a datos fuera del EDT: persiste `EN_REPARTO`, espera el tiempo definido por `calcularPausaEntregaMs()` y finalmente llama a `registrarEntregaCompleta()`, que en **una sola transacción** cambia el pedido a `ENTREGADO` e inserta la fila en `entrega`. Si esa transacción falla, se deshace por completo y el diálogo informa que no se completó la entrega. En `done()` el pedido se marca como entregado en memoria, se registra en el `ControladorDeEnvios` junto con el nombre del repartidor, se notifica a las tablas abiertas y el diálogo se cierra.

## Base de datos

MySQL corre en Docker con el puerto 3306 publicado en el host.

```bash
docker run -d --name speedfast-mysql -e MYSQL_ROOT_PASSWORD=desarrollo \
  -e MYSQL_DATABASE=speedfast_db -p 3306:3306 mysql:latest
```

### Esquema

`sql/esquema.sql` construye la base completa desde cero —base, tablas, claves primarias y foráneas— y es idempotente (`IF NOT EXISTS`), así que puede ejecutarse las veces que haga falta sin duplicar objetos ni perder datos.

| Tabla | Columnas |
|---|---|
| `pedido` | `id` (AI, PK), `direccion`, `tipo`, `estado`, `distancia_km`, `mochila_termica`, `peso`, `embalaje_validado`, `distancia_repartidor_cercano` |
| `repartidor` | `id` (AI, PK), `nombre` (`UNIQUE`) |
| `entrega` | `id` (AI, PK), `id_pedido` (FK → `pedido`), `id_repartidor` (FK → `repartidor`), `fecha` (DATE), `hora` (TIME) |

Además, `pedido` restringe con `CHECK` los dominios de `tipo` (`Comida`, `Encomienda`, `Express`) y `estado` (`PENDIENTE`, `EN_REPARTO`, `ENTREGADO`), y ambas FK de `entrega` declaran `ON DELETE CASCADE`, de modo que borrar un pedido o un repartidor arrastra sus entregas en vez de violar la restricción.

Los scripts se aplican dentro del contenedor, en este orden:

```powershell
Get-Content -Raw -Encoding UTF8 sql/esquema.sql         | docker exec -i speedfast-mysql mysql -u root -pdesarrollo
Get-Content -Raw -Encoding UTF8 sql/datos_iniciales.sql | docker exec -i speedfast-mysql mysql -u root -pdesarrollo
```

> **Nota**: el nombre de la base es `speedfast_db` en los scripts, en la URL de `ConexionDB` y en esta documentación. Si tu contenedor se creó con el nombre anterior (`speedfastdb`), créalo de nuevo con el comando de arriba o renombra la base con `RENAME DATABASE speedfastdb TO speedfast_db;`.

### Credenciales

Viven hardcodeadas en `persistencia/ConexionDB.java` (`speedfast_db`, usuario `root`). La URL incluye tres parámetros que no son opcionales:

```java
jdbc:mysql://localhost:3306/speedfast_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=America/Santiago
```

`allowPublicKeyRetrieval=true` es **imprescindible**: el usuario `root` usa `caching_sha2_password` y la conexión es TCP sin SSL, así que sin este parámetro el driver lanza `Public Key Retrieval is not allowed`.

## Requisitos

- JDK 8 o superior (el proyecto compila con Java 26).
- Docker, con el contenedor de MySQL en ejecución.
- **MySQL Connector/J 26.7.0** en `lib/mysql-connector-j-26.7.0.jar` (la carpeta `lib/` está en `.gitignore`; descárgala desde Maven Central si no está).

## Cómo compilar y ejecutar

1. Clona o abre el proyecto en tu máquina.
2. Levanta MySQL en Docker, crea el esquema y carga los repartidores de ejemplo (ver [Base de datos](#base-de-datos)):

   ```powershell
   docker run -d --name speedfast-mysql -e MYSQL_ROOT_PASSWORD=desarrollo -e MYSQL_DATABASE=speedfast_db -p 3306:3306 mysql:latest
   Get-Content -Raw -Encoding UTF8 sql/esquema.sql         | docker exec -i speedfast-mysql mysql -u root -pdesarrollo
   Get-Content -Raw -Encoding UTF8 sql/datos_iniciales.sql | docker exec -i speedfast-mysql mysql -u root -pdesarrollo
   ```

3. Desde la raíz del proyecto (`SpeedFast/`), compila las clases incluyendo el driver en el classpath:

   ```powershell
   javac -encoding UTF-8 -cp "lib/mysql-connector-j-26.7.0.jar" -d out -sourcepath src (Get-ChildItem src -Recurse -Filter *.java).FullName
   ```

4. Ejecuta la interfaz gráfica:

   ```powershell
   java -cp "out;lib/mysql-connector-j-26.7.0.jar" main.Main
   ```

> En Windows el separador del classpath es `;`. En Linux o macOS usa `:` y enclose las rutas del driver entre comillas.

### Desde IntelliJ IDEA

1. Abre el proyecto con IntelliJ IDEA. El driver de `lib/` ya está declarado en `SpeedFast.iml`, y `src/` es la única raíz de fuentes.
2. Ejecuta `main.Main` con el botón **Run** o `Shift+F10`.
