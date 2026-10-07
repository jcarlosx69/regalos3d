-- Gastos varios (iluminación, mecanismos...) y precio final ajustable a mano.
--   varios_*      : se suman al coste calculado
--   coste_manual  : si tiene valor, sustituye al coste calculado como precio final del regalo

ALTER TABLE regalo
  ADD COLUMN varios_concepto VARCHAR(150) NULL AFTER precio_kwh,
  ADD COLUMN varios_importe  DECIMAL(8,2) NULL AFTER varios_concepto,
  ADD COLUMN coste_manual    DECIMAL(8,2) NULL AFTER coste_luz;

CREATE OR REPLACE VIEW v_regalo_coste AS
SELECT r.id,
       r.persona_id,
       r.modelo_id,
       r.fecha,
       r.ocasion,
       COALESCE(r.coste_luz, 0)                                        AS coste_luz,
       COALESCE(f.total, 0)                                            AS coste_filamento,
       COALESCE(r.varios_importe, 0)                                   AS coste_varios,
       COALESCE(r.coste_luz, 0) + COALESCE(f.total, 0)
         + COALESCE(r.varios_importe, 0)                               AS coste_calculado,
       COALESCE(r.coste_manual,
                COALESCE(r.coste_luz, 0) + COALESCE(f.total, 0)
                  + COALESCE(r.varios_importe, 0))                     AS coste_total
FROM regalo r
LEFT JOIN (SELECT regalo_id, SUM(coste) AS total
           FROM regalo_filamento
           GROUP BY regalo_id) f ON f.regalo_id = r.id;
