-- Coste total de cada regalo (luz + filamentos), base de los informes.
CREATE VIEW v_regalo_coste AS
SELECT r.id,
       r.persona_id,
       r.modelo_id,
       r.fecha,
       r.ocasion,
       COALESCE(r.coste_luz, 0)                                   AS coste_luz,
       COALESCE((SELECT SUM(f.coste) FROM regalo_filamento f
                 WHERE f.regalo_id = r.id), 0)                    AS coste_filamento,
       COALESCE(r.coste_luz, 0)
         + COALESCE((SELECT SUM(f.coste) FROM regalo_filamento f
                     WHERE f.regalo_id = r.id), 0)                AS coste_total
FROM regalo r;
