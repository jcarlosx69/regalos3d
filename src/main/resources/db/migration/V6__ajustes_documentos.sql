-- Ajustes de los documentos PDF: días de validez del presupuesto y un texto libre para el pie
-- (condiciones, plazo de entrega, agradecimiento...).
ALTER TABLE ajustes
    ADD COLUMN validez_presupuesto_dias INT          NOT NULL DEFAULT 30 AFTER margen_pct,
    ADD COLUMN texto_pie                VARCHAR(500) NULL               AFTER validez_presupuesto_dias;
