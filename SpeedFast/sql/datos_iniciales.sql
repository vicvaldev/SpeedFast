-- =============================================================
-- SpeedFast - Datos iniciales
-- =============================================================
-- Repartidores de ejemplo. Es necesario al menos uno para poder
-- asignar pedidos desde la interfaz grafica.
--
-- INSERT IGNORE + la restriccion uq_repartidor_nombre hacen que el
-- script sea idempotente: se puede volver a ejecutar sin duplicar.
--
-- Ejecutar dentro del contenedor Docker:
--   Get-Content -Raw -Encoding UTF8 sql/datos_iniciales.sql |
--       docker exec -i speedfast-mysql mysql -u root -pdesarrollo
-- =============================================================

USE speedfast_db;

INSERT IGNORE INTO repartidor (nombre) VALUES
    ('Camila Rojas'),
    ('Diego Perez'),
    ('Sofia Contreras');
