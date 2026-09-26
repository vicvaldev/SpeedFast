-- =============================================================
-- SpeedFast - Migracion: columnas de detalle del pedido
-- =============================================================
-- La tabla `pedido` original solo guardaba los datos comunes de la
-- clase base (id, direccion, tipo, estado). Para que el estado
-- interno de cada subclase (PedidoComida, PedidoEncomienda,
-- PedidoExpress) sobreviva a un ciclo guardado -> lectura, se
-- agregan columnas nullable que el PedidoDAO rellena segun el tipo.
--
-- Nullable a proposito: las filas existentes no deben invalidarse.
--
-- Ejecutar dentro del contenedor Docker:
--   docker exec -i cool_mcnulty mysql -u root -pdesarrollo < sql/migracion_detalle_pedido.sql
-- =============================================================

USE speedfastdb;

-- Distancia en km que recorre el pedido (columna comun de Pedido).
ALTER TABLE pedido
    ADD COLUMN distancia_km DECIMAL(6, 2) NULL;

-- PedidoComida: requiere mochila termica para validar la entrega.
ALTER TABLE pedido
    ADD COLUMN mochila_termica TINYINT(1) NULL;

-- PedidoEncomienda: peso en kg y validacion del embalaje.
ALTER TABLE pedido
    ADD COLUMN peso DECIMAL(6, 2) NULL;

ALTER TABLE pedido
    ADD COLUMN embalaje_validado TINYINT(1) NULL;

-- PedidoExpress: distancia hasta el repartidor mas cercano.
ALTER TABLE pedido
    ADD COLUMN distancia_repartidor_cercano DECIMAL(6, 2) NULL;
