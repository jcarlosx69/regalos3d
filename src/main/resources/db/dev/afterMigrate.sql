-- Datos INVENTADOS de desarrollo (perfil dev). Flyway ejecuta este script después de cada migración,
-- así que es idempotente: INSERT IGNORE con id fijos (9001+) no duplica nada en arranques posteriores.
-- Los importes están calculados con las tarifas por defecto de V4 (0,15 €/kWh, 150 W,
-- 12 €/h mano de obra, 0,25 €/h amortización, 30 % de margen) y coinciden con CalculadoraPrecio.

INSERT IGNORE INTO cliente (id, telefono, nombre, email, direccion, notas) VALUES
  (9001, '+34600900001', 'Taller Demo S.L.', 'compras@taller-demo.example', 'C/ Inventada 1, 35001 Las Palmas de G.C.', NULL),
  (9002, '+34600900002', 'Cliente Prueba', NULL, NULL, 'Prefiere PETG');

INSERT IGNORE INTO modelo (id, manyfold_model_id, nombre, gramos_estimados) VALUES
  (9001, 'demo-llavero', 'Llavero personalizado', 8.00),
  (9002, 'demo-soporte', 'Soporte de móvil', 45.00);

-- Número correlativo dentro del año, calculado al insertar para no chocar con encargos reales
INSERT IGNORE INTO encargo (id, anio, numero, cliente_id, tipo, estado, fecha, fecha_entrega, fecha_cobro, ocasion, notas,
                            incluir_mano_obra, incluir_margen, potencia_w, precio_kwh, mano_obra_hora, amortizacion_hora,
                            margen_pct, coste, mano_obra, margen, precio_calculado, precio_manual, precio_final)
SELECT 9001, 2026, COALESCE(MAX(numero), 0) + 1, 9001, 'VENTA', 'IMPRIMIENDO', '2026-10-01', NULL, NULL, NULL,
       'Llaveros con el logo del taller', 1, 1, 150.0, 0.1500, 12.00, 0.25, 30.00, 4.90, 15.00, 5.97, 25.87, NULL, 25.87
FROM encargo WHERE anio = 2026 AND NOT EXISTS (SELECT 1 FROM encargo WHERE id = 9001);

INSERT IGNORE INTO encargo (id, anio, numero, cliente_id, tipo, estado, fecha, fecha_entrega, fecha_cobro, ocasion, notas,
                            incluir_mano_obra, incluir_margen, potencia_w, precio_kwh, mano_obra_hora, amortizacion_hora,
                            margen_pct, coste, mano_obra, margen, precio_calculado, precio_manual, precio_final)
SELECT 9002, 2026, COALESCE(MAX(numero), 0) + 1, 9002, 'VENTA', 'PRESUPUESTO', '2026-10-05', NULL, NULL, NULL,
       NULL, 1, 1, 150.0, 0.1500, 12.00, 0.25, 30.00, 3.80, 6.00, 2.94, 12.74, NULL, 12.74
FROM encargo WHERE anio = 2026 AND NOT EXISTS (SELECT 1 FROM encargo WHERE id = 9002);

INSERT IGNORE INTO encargo (id, anio, numero, cliente_id, tipo, estado, fecha, fecha_entrega, fecha_cobro, ocasion, notas,
                            incluir_mano_obra, incluir_margen, potencia_w, precio_kwh, mano_obra_hora, amortizacion_hora,
                            margen_pct, coste, mano_obra, margen, precio_calculado, precio_manual, precio_final)
SELECT 9003, 2026, COALESCE(MAX(numero), 0) + 1, 9001, 'VENTA', 'COBRADO', '2026-09-10', '2026-09-15', '2026-09-20', NULL,
       'Descuento por volumen', 1, 1, 150.0, 0.1500, 12.00, 0.25, 30.00, 6.00, 24.00, 9.00, 39.00, 35.00, 35.00
FROM encargo WHERE anio = 2026 AND NOT EXISTS (SELECT 1 FROM encargo WHERE id = 9003);

-- Llavero: 8 g PLA a 20 €/kg = 0,16 · energía 0,5 h = 0,01 · amortización 0,5 h = 0,13 → coste 0,30
--          mano de obra 0,1 h = 1,20 · margen 30 % de 1,50 = 0,45 → precio 1,95
-- Soporte: 45 g PETG a 24 €/kg = 1,08 · energía 3 h = 0,07 · amortización 3 h = 0,75 → coste 1,90
--          mano de obra 0,25 h = 3,00 · margen 30 % de 4,90 = 1,47 → precio 6,37
INSERT IGNORE INTO encargo_linea (id, encargo_id, posicion, modelo_id, cantidad, horas_impresion, horas_mano_obra,
                                  varios_concepto, varios_importe, coste_unitario, precio_unitario, importe) VALUES
  (9001, 9001, 1, 9001, 10, 0.50, 0.10, NULL, NULL, 0.30, 1.95, 19.50),
  (9002, 9001, 2, 9002,  1, 3.00, 0.25, NULL, NULL, 1.90, 6.37,  6.37),
  (9003, 9002, 1, 9002,  2, 3.00, 0.25, NULL, NULL, 1.90, 6.37, 12.74),
  (9004, 9003, 1, 9001, 20, 0.50, 0.10, NULL, NULL, 0.30, 1.95, 39.00);

INSERT IGNORE INTO linea_filamento (id, linea_id, material, color, gramos, precio_kg) VALUES
  (9001, 9001, 'PLA',  'Negro', 8.00, 20.00),
  (9002, 9002, 'PETG', 'Gris', 45.00, 24.00),
  (9003, 9003, 'PETG', 'Gris', 45.00, 24.00),
  (9004, 9004, 'PLA',  'Negro', 8.00, 20.00);
