# SpeedFast

Sistema de reparto de pedidos que gestiona de forma integral diferentes tipos de envíos, aplicando conceptos de **Programación Orientada a Objetos** (polimorfismo, abstracción e interfaces), de **Programación Concurrente** (`synchronized` con `wait`/`notifyAll`, `ReentrantLock`, `AtomicInteger` y `SwingWorker`) y de **persistencia en base de datos** (JDBC con el patrón DAO).

## Descripción del proyecto

SpeedFast administra pedidos diferenciados por tipo — **Comida**, **Encomienda** y **Express** — cada uno con lógica específica de asignación de repartidor y cálculo del tiempo de entrega. El historial de entregas ya no vive en memoria: se persiste en MySQL y se muestra en una tabla dentro de la propia interfaz, desde la ventana de gestión de entregas.

La clase `ControladorDeEnvios` sigue implementing los contratos `Despachable`, `Cancelable` y `Rastreable` —reservar, despachar, cancelar y ver el historial— como API de dominio disponible, pero **la interfaz solo la usa para registrar entregas**: el resto de métodos quedan a disposición del modelo y no tienen un acceso en la GUI. El reparto real lo resuelve `DialogoAsignarRepartidor`, no el historial en memoria.

El paquete de modelos incluye además una simulación concurrente con patrón **productor-consumidor**: los `GeneradorDePedidos` cargan pedidos sobre una `ZonaDeCarga` acotada mientras los `Repartidor` (consumidores) los retiran y entregan en paralelo, sincronizando el acceso a la sección crítica. Esa simulación está disponible como biblioteca (`ZonaDeCarga`, `GeneradorDePedidos`, `Repartidor.run()`) y **no está conectada al flujo de la interfaz**: nada en `main.Main` la instancia, y el reparto de la aplicación se resuelve con `SwingWorker`.

La **interfaz gráfica (Swing)** implementa el **CRUD completo** de las tres entidades —pedidos, repartidores y entregas— en una sola pantalla por entidad: listar en tabla, registrar, editar y eliminar, con filtros opcionales y validación de entradas antes de tocar la base de datos. Los datos viven en **MySQL**: el paquete `persistencia` encapsula conexiones y operaciones, y el paquete `controladores` es el único puente hacia ellos, de modo que las vistas nunca construyen conexiones ni conocen DAOs por su cuenta.

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
│   │   └── ControladorDePedidos.java  (fachada CRUD de pedidos, repartidores y entregas; la inyectan las vistas)
│   ├── persistencia/
│   │   ├── ConexionDB.java            (conexión a MySQL y carga del driver)
│   │   ├── PedidoDAO.java             (create, readAll, readPendientes, readFiltrado, readPorId, update×3, delete, existe)
│   │   ├── RepartidorDAO.java         (create, readAll, update, delete, existeNombre)
│   │   └── EntregaDAO.java            (create×2, readAll, readPorPedido/Repartidor, update, delete, contarPor*, readDetalle×3)
│   ├── vistas/
│   │   ├── VentanaPrincipal.java          (JFrame principal: cuatro accesos y Salir)
│   │   ├── VentanaListado.java            (clase base de las tablas CRUD)
│   │   ├── VentanaListaPedidos.java       (alta, JTable con filtros, edición y eliminación)
│   │   ├── VentanaListaRepartidores.java  (alta, JTable de repartidores, edición y eliminación)
│   │   ├── VentanaListaEntregas.java      (alta, JTable de entregas, filtros y edición)
│   │   ├── DialogoPedido.java             (JDialog: alta y edición de un pedido)
│   │   ├── DialogoRepartidor.java         (JDialog: alta y edición de un repartidor)
│   │   ├── DialogoEntrega.java            (JDialog: alta y edición de una entrega)
│   │   └── DialogoAsignarRepartidor.java  (JDialog: asignar repartidor / iniciar entrega)
│   └── modelos/
│       ├── contratos/
│       │   ├── Despachable.java       (interfaz: despachar)
│       │   ├── Cancelable.java        (interfaz: cancelar)
│       │   └── Rastreable.java        (interfaz: verHistorial)
│       └── implementacion/
│           ├── Pedido.java            (clase abstracta base)
│           ├── PedidoFabrica.java     (fábrica: crea la subclase según el tipo)
│           ├── EstadoPedido.java      (enum: PENDIENTE, EN_REPARTO, ENTREGADO)
│           ├── PedidoComida.java      (subclase Comida)
│           ├── PedidoEncomienda.java  (subclase Encomienda)
│           ├── PedidoExpress.java     (subclase Express)
│           ├── Entrega.java           (entidad de dominio de la relación pedido-repartidor)
│           ├── DetalleEntrega.java    (proyección de la tabla: entrega + pedido + repartidor)
│           ├── ZonaDeCarga.java       (buffer acotado: synchronized + wait/notifyAll)
│           ├── GeneradorDePedidos.java (productor, implementa Runnable)
│           ├── RegistroEntrega.java   (DTO inmutable de una entrada del historial en memoria)
│           ├── ControladorDeEnvios.java (historial con ReentrantLock y contador AtomicInteger)
│           └── Repartidor.java        (doble rol: entidad de la BD e hilo consumidor de ZonaDeCarga)
```

## Conceptos aplicados

- **Polimorfismo**: jerarquía con clase base `Pedido` y subclases `PedidoComida`, `PedidoEncomienda` y `PedidoExpress`. Método sobrescrito `asignarRepartidor()` en cada subclase y método sobrecargado `asignarRepartidor(String nombre)`.
- **Abstracción**: clase abstracta `Pedido` con atributos (`idPedido`, `direccionEntrega`, `distanciaKm`), método implementado `mostrarResumen()` y método abstracto `calcularTiempoEntrega()` con lógica personalizada en cada subclase.
- **Interfaces**: `Despachable`, `Cancelable` y `Rastreable`, implementadas por la clase `ControladorDeEnvios`, que también mantiene el historial de entregas en un `ArrayList` de `RegistroEntrega`.
- **Arquitectura por capas e inyección de dependencias**: `Main` actúa como *composition root* y construye `ControladorDePedidos` y `ControladorDeEnvios`, que se inyectan por constructor en `VentanaPrincipal` y, desde ahí, en cada ventana hija. Ninguna vista importa el paquete `persistencia`: la colección de pedidos ya no vive en `VentanaPrincipal` sino detrás de `ControladorDePedidos`, que es el único punto que conoce los DAO. Cambiar el mecanismo de almacenamiento solo obliga a modificar ese controlador.
- **Concurrencia y sincronización (productor-consumidor)**: la `ZonaDeCarga` es un buffer acotado bloqueante. Sus métodos `agregarPedido()` (productores) y `retirarPedido()` (consumidores) están declarados como `synchronized` y usan `wait()`/`notifyAll()`: los productores esperan cuando la zona está llena y los consumidores cuando está vacía; `cerrarProduccion()` cuenta productores activos y despierta a los consumidores solo cuando no habrá más pedidos. El patrón se completa con varios `GeneradorDePedidos` preparando pedidos en paralelo mientras las instancias de `Repartidor` los retiran y entregan, simulando el tiempo con `Thread.sleep()` fuera de la sección crítica. `ControladorDeEnvios` protege su historial compartido con `ReentrantLock` y lleva el contador de entregas con `AtomicInteger`. El alcance de esta simulación es el del paquete de modelos: `main.Main` no instancia `ZonaDeCarga` ni genera hilos, la interfaz no la invoca, y su propio reparto se resuelve con `SwingWorker` (ver *Thread safety en la GUI*).
- **Interfaz gráfica (Swing)**: las ventanas del paquete `vistas` emplean `JFrame`, `JDialog`, `JTable` con `DefaultTableModel` (celdas no editables) y `JOptionPane` para validación y confirmación. `VentanaPrincipal` conserva el patrón observador (`agregarActualizador`/`quitarActualizador`) para que las tablas abiertas se actualicen solas.
- **Persistencia (patrón DAO)**: el paquete `persistencia` concentra todo el acceso a datos. `ConexionDB` es el único punto que conoce URL y credenciales; los DAO usan `PreparedStatement` y `ResultSet` siempre dentro de **try-with-resources**, de modo que la conexión y el statement se cierren aunque la operación falle. Cada DAO expone las cuatro operaciones del CRUD con nombres en inglés (`create`, `readAll`, `update`, `delete`) más las consultas especializadas que necesita la interfaz. `RepartidorDAO.create()` y `EntregaDAO.create()` recuperan la clave generada con `Statement.RETURN_GENERATED_KEYS` y la devuelven ya asignada en el objeto.
- **CRUD completo con validación**: cada alta, edición y borrado se valida en la vista antes de invocar al controlador (campos no vacíos, ID numérico, distancia positiva, nombre de repartidor único) y de nuevo en la capa de datos, donde `RepartidorDAO.existeNombre()` recibe el identificador que se está editando para que un repartidor no entre en conflicto consigo mismo al renombrarse. La unique de `repartidor.nombre` actúa como última línea de defensa ante una carrera.
- **Transacciones**: cuando una acción de negocio toca más de una tabla, el controlador agrupa las escrituras en una única transacción en lugar de delegarlas en DAO independientes. `ControladorDePedidos.registrarEntregaCompleta()` abre una conexión, desactiva el autocommit y ejecuta el `UPDATE pedido SET estado = 'ENTREGADO'` junto con el `INSERT` en `entrega` mediante las sobrecargas transaccionales `PedidoDAO.update(Connection, int, EstadoPedido)` y `EntregaDAO.create(Connection, Entrega)`, que reciben la conexión del llamador en vez de abrir la suya —frente a las sobrecargas sin `Connection`, que abren la suya y se usan en los cambios de estado sueltos. Solo si ambas terminan bien se confirma el `commit()`; ante cualquier fallo se hace `rollback()` y la excepción original se propaga hacia la vista. Así el pedido nunca queda marcado como entregado sin su entrega asociada, ni al revés.
- **Mapeo objeto-relacional con polimorfismo**: la tabla `pedido` usa una columna discriminadora (`tipo`) más una columna nullable por atributo propio de cada subclase (`mochila_termica`, `peso`, `embalaje_validado`, `distancia_repartidor_cercano`). `PedidoDAO.crearPedido(ResultSet)` reconstruye la subclase correcta al leer, de modo que un pedido sobrevive intacto a un ciclo `create` → `readPorId`, incluso si la edición cambió su tipo. Como esas columnas son nullable, un `NULL` se interpreta con el mismo valor que aplica el formulario, para que el round-trip no termine rechazando la entrega en `validarEntrega()`.
- **Consultas compuestas**: `EntregaDAO.readDetalle()` resuelve los tres `readDetalle*` con un `LEFT JOIN` sobre `pedido` y `repartidor` y proyecta el resultado en `DetalleEntrega`, de modo que la tabla de entregas muestre tipo, dirección, repartidor y estado sin una consulta por fila ni depender de que el objeto completo esté cargado en memoria.
- **Thread safety en la GUI**: **ninguna operación JDBC corre en el Event Dispatch Thread**. `Main` agenda la creación de la ventana con `SwingUtilities.invokeLater()`, y cada consulta o escritura se ejecuta dentro de un `SwingWorker` (`doInBackground`), pintando el resultado solo en `done()`. Los `JOptionPane` de confirmación y error también se invocan desde `done()`.

## Interfaz gráfica

La aplicación se inicia desde `main.Main`, que construye los controladores y agenda la apertura de `VentanaPrincipal` con `SwingUtilities.invokeLater()`, de modo que la interfaz se crea explícitamente en el Event Dispatch Thread. La comprobación de la base de datos (`verificarDisponibilidad()`) corre en un `SwingWorker`: si MySQL no responde, el motivo se muestra en un `JOptionPane` en vez de dejar ventanas vacías sin explicación. El menú principal ofrece cuatro accesos, uno por entidad gestionada, más **Salir**:

1. **Gestionar pedidos**: abre `VentanaListaPedidos`, una `JTable` de solo lectura (ID, Tipo, Dirección, Estado, Distancia (km)) alimentada por `listarPedidosFiltrados(tipo, estado)`. Concentra **todo** el ciclo de vida del pedido en una sola pantalla: **Registrar** abre `DialogoPedido` en modo alta (el usuario elige el ID y el estado inicial es `PENDIENTE`, con validación de duplicados), **Editar** reabre ese mismo diálogo cargado con la fila seleccionada y con el ID deshabilitado por ser la clave del `UPDATE`, y **Eliminar** borra el pedido previa confirmación que advierte de que sus entregas también se borrarán en cascada. Los desplegables de tipo y estado pueden dejarse en «Todos» o combinarse entre sí, y **Quitar filtro** los restablece. Se refresca sola cuando otra ventana notifica un cambio.
2. **Gestionar repartidores**: abre `VentanaListaRepartidores`, con la misma estructura (ID, Nombre). **Registrar** y **Editar** usan `DialogoRepartidor`, y **Eliminar** muestra un aviso que menciona también la cascada de pedidos y entregas asociadas.
3. **Gestionar entregas**: abre `VentanaListaEntregas`, una `JTable` de solo lectura (ID, Pedido, Tipo, Dirección, Repartidor, Fecha, Hora) alimentada por `EntregaDAO.readDetalle()`. A diferencia del antiguo historial, esta ventana **sí consulta la base de datos**, y el filtro de dos niveles permite acotar por un pedido concreto o por un repartidor. **Registrar entrega** y **Editar** usan `DialogoEntrega`.
4. **Asignar repartidor / Iniciar entrega**: abre `DialogoAsignarRepartidor`, que carga los pedidos pendientes (`listarPedidosPendientes()`) y los repartidores (`listarRepartidores()`) en un único `SwingWorker` de arranque. Se elige un pedido y un repartidor, y se invoca `asignarRepartidor(nombre)`. El estado del pedido cambia según las validaciones existentes (`validarEntrega()`): si supera las condiciones (mochila térmica en Comida, peso ≤ 20 kg y embalaje validado en Encomienda, siempre válido en Express), la asignación se acepta; en caso contrario el pedido permanece `PENDIENTE` y se muestra **Asignación rechazada**. Tras la aceptación, la entrega se simula en segundo plano con un `SwingWorker` que hace todo el acceso a datos fuera del EDT: persiste `EN_REPARTO` con `cambiarEstadoPedido()`, espera el tiempo definido por `calcularPausaEntregaMs()` y finalmente llama a `registrarEntregaCompleta()`, que en **una sola transacción** cambia el pedido a `ENTREGADO` e inserta la fila en `entrega`. Si esa transacción falla, se deshace por completo y el diálogo informa que no se completó la entrega. En `done()` se registra la entrega en el `ControladorDeEnvios`, se notifica a las tablas abiertas y el diálogo se cierra.
5. **Salir**: cierra la aplicación.

### Notas de diseño de las ventanas

- **No hay ventanas de registro separadas**: cada entidad se gestiona desde una única pantalla que concentra listado, alta, edición y eliminación. Para evitar duplicar formularios, `DialogoPedido` y `DialogoRepartidor` cubren a la vez el alta y la edición mediante factorías `nuevo(...)` y `editar(...)` —el mismo patrón de `DialogoEntrega`—, y el constructor privado decide el modo a partir de si el objeto recibido es `null`. Lo único que cambia entre modos es qué campo queda congelado y si hay que descartar duplicados.
- Las tres tablas de listado comparten la clase base `VentanaListado<T>`, que centraliza el `JTable` con su `DefaultTableModel` (celdas no editables), el `SwingWorker` de carga, la notificación de cambios y el conjunto de acciones de la barra inferior. Cada subclase solo aporta sus columnas, sus filtros y sus diálogos.

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

> **Nota**: los comandos de arriba usan el nombre de contenedor `speedfast-mysql`. Si tu instancia se llama de otro modo —por ejemplo `cool_mcnulty`, que es como Docker la bautizó al crearla sin `--name`— sustituye ese identificador; `docker ps` muestra el nombre real.

### Credenciales

Viven hardcodeadas en `persistencia/ConexionDB.java` (`speedfast_db`, usuario `root`). La URL incluye tres parámetros que no son opcionales:

```java
jdbc:mysql://localhost:3306/speedfast_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=America/Santiago
```

`allowPublicKeyRetrieval=true` es **imprescindible**: el usuario `root` usa `caching_sha2_password` y la conexión es TCP sin SSL, así que sin este parámetro el driver lanza `Public Key Retrieval is not allowed`.

## Requisitos

- JDK 8 o superior (el proyecto compila con Java 26 y también verificado con `--release 8`).
- Docker, con el contenedor de MySQL en ejecución.
- **MySQL Connector/J 26.7.0** en `lib/mysql-connector-j-26.7.0.jar` (la carpeta `lib/` está en `.gitignore`; descárgala desde Maven Central si no está).

> Si `javac` no está en el `PATH` —frecuente cuando el JDK se instaló desde IntelliJ o desde un gestor de versiones— llama al ejecutable por su ruta absoluta, por ejemplo `& "C:\ruta\jdk-26\bin\javac.exe" ...`, o agrega `...\bin` al `PATH` de la sesión.

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
