-- =============================================================
-- SpeedFast - Datos iniciales
-- =============================================================
-- Repartidores de ejemplo. Es necesario al menos uno para poder
-- asignar pedidos desde la interfaz grafica.
--
-- Ejecutar dentro del contenedor Docker:
--   docker exec -i cool_mcnulty mysql -u root -pdesarrollo < sql/datos_iniciales.sql
-- =============================================================

USE speedfastdb;

INSERT INTO repartidor (nombre) VALUES
    ('Camila Rojas'),
    ('Diego Perez'),
    ('Sofia Contreras');
