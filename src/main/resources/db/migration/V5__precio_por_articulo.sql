-- Precio por artículo: cada línea puede llevar un precio por unidad fijado a mano, o ser un obsequio
-- (regalo o promoción dentro del encargo) que no se cobra. El coste del obsequio sigue contando.
ALTER TABLE encargo_linea
    ADD COLUMN obsequio      BIT(1)        NOT NULL DEFAULT 0 AFTER varios_importe,
    ADD COLUMN precio_manual DECIMAL(10,2) NULL               AFTER obsequio;
