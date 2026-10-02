-- =============================================================
-- SpeedFast - Esquema de la base de datos
-- =============================================================
-- Script que CONSTRUYE la base completa y sus relaciones desde cero.
-- Es idempotente (IF NOT EXISTS), por lo que puede ejecutarse varias
-- veces sin duplicar objetos ni perder los datos existentes.
--
-- Este archivo reemplaza a la antigua migracion de columnas de
-- detalle: aqui la tabla `pedido` ya nace con todas las columnas
-- nullable que necesita la jerarquia polimorfica.
--
-- El nombre de la base es speedfast_db, el mismo que usa la URL de
-- ConexionDB.java, para que la configuracion y la documentacion
-- coincidan y no haya confusiones al preparar el entorno.
--
-- Ejecutar dentro del contenedor Docker:
--   Get-Content -Raw -Encoding UTF8 sql/esquema.sql |
--       docker exec -i speedfast-mysql mysql -u root -pdesarrollo
-- =============================================================

CREATE DATABASE IF NOT EXISTS speedfast_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE speedfast_db;

-- -------------------------------------------------------------
-- repartidor: catalogo de los repartidores disponibles.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS repartidor (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    -- La aplicacion valida el repetido con existeNombre() antes de
    -- insertar; la restriccion es la garantia de ultimo nivel.
    CONSTRAINT uq_repartidor_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

-- -------------------------------------------------------------
-- pedido: tabla base de la jerarquia polimorfica.
--
-- La columna `tipo` es la discriminadora (Comida, Encomienda,
-- Express) y las columnas de detalle son nullable a proposito: solo
-- aplican a una subclase y, cuando llegan en NULL desde una fila
-- antigua, PedidoDAO.crearPedido() las interpreta con el mismo valor
-- por defecto que aplica el formulario.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pedido (
    id                           INT AUTO_INCREMENT PRIMARY KEY,
    direccion                    VARCHAR(160) NOT NULL,
    tipo                         VARCHAR(20)  NOT NULL,
    estado                       VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    -- Columna comun de Pedido (clase base).
    distancia_km                 DECIMAL(6, 2) NULL,
    -- Detalle de PedidoComida: requiere mochila termica para validar.
    mochila_termica              TINYINT(1)   NULL,
    -- Detalle de PedidoEncomienda: peso en kg y validacion del embalaje.
    peso                         DECIMAL(6, 2) NULL,
    embalaje_validado            TINYINT(1)   NULL,
    -- Detalle de PedidoExpress: distancia hasta el repartidor mas cercano.
    distancia_repartidor_cercano DECIMAL(6, 2) NULL,
    CONSTRAINT chk_pedido_tipo   CHECK (tipo   IN ('Comida', 'Encomienda', 'Express')),
    CONSTRAINT chk_pedido_estado CHECK (estado IN ('PENDIENTE', 'EN_REPARTO', 'ENTREGADO'))
) ENGINE = InnoDB;

-- -------------------------------------------------------------
-- entrega: materializa la relacion N a 1 entre un pedido entregado
-- y el repartidor que lo realizo. La fecha y la hora se guardan en
-- columnas separadas (DATE / TIME), igual que el DTO Entrega.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entrega (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido     INT NOT NULL,
    id_repartidor INT NOT NULL,
    fecha         DATE NOT NULL,
    hora          TIME NOT NULL,
    CONSTRAINT fk_entrega_pedido FOREIGN KEY (id_pedido)
        REFERENCES pedido (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_entrega_repartidor FOREIGN KEY (id_repartidor)
        REFERENCES repartidor (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;
