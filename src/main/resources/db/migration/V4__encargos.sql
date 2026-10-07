-- Fase 1 del modelo comercial: clientes, encargos con varias líneas, estados y precio de venta.
--
-- Los regalos existentes pasan a ser encargos de tipo REGALO en estado ENTREGADO, con su coste intacto.
-- Las tablas antiguas NO se borran: quedan como legacy_regalo y legacy_regalo_filamento, sin claves
-- externas hacia fuera, para poder comprobar el paso de datos. Se eliminarán en una migración posterior.

-- ---------------------------------------------------------------- clientes
RENAME TABLE persona TO cliente;

ALTER TABLE cliente
  ADD COLUMN email     VARCHAR(150) NULL AFTER nombre,
  ADD COLUMN direccion VARCHAR(300) NULL AFTER email;

-- ---------------------------------------------------------------- tarifas por defecto (una sola fila)
CREATE TABLE ajustes (
  id                 INT          NOT NULL PRIMARY KEY,
  precio_kg          DECIMAL(6,2) NOT NULL,
  precio_kwh         DECIMAL(6,4) NOT NULL,
  potencia_w         DECIMAL(6,1) NOT NULL,
  mano_obra_hora     DECIMAL(6,2) NOT NULL,
  amortizacion_hora  DECIMAL(6,2) NOT NULL,
  margen_pct         DECIMAL(5,2) NOT NULL,
  CONSTRAINT ck_ajustes_unica_fila CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ajustes (id, precio_kg, precio_kwh, potencia_w, mano_obra_hora, amortizacion_hora, margen_pct)
VALUES (1, 20.00, 0.1500, 150.0, 12.00, 0.25, 30.00);

-- ---------------------------------------------------------------- encargos
-- Las tarifas se copian en cada encargo al crearlo: cambiar los ajustes no altera encargos anteriores.
-- Los importes calculados se guardan para listados, informes y documentos.
CREATE TABLE encargo (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  anio               INT           NOT NULL,
  numero             INT           NOT NULL,
  cliente_id         BIGINT        NOT NULL,
  tipo               VARCHAR(10)   NOT NULL,
  estado             VARCHAR(12)   NOT NULL,
  fecha              DATE          NOT NULL,
  fecha_entrega      DATE          NULL,
  fecha_cobro        DATE          NULL,
  ocasion            VARCHAR(100)  NULL,
  notas              VARCHAR(1000) NULL,
  incluir_mano_obra  BIT(1)        NOT NULL,
  incluir_margen     BIT(1)        NOT NULL,
  potencia_w         DECIMAL(6,1)  NOT NULL,
  precio_kwh         DECIMAL(6,4)  NOT NULL,
  mano_obra_hora     DECIMAL(6,2)  NOT NULL,
  amortizacion_hora  DECIMAL(6,2)  NOT NULL,
  margen_pct         DECIMAL(5,2)  NOT NULL,
  coste              DECIMAL(10,2) NOT NULL DEFAULT 0,
  mano_obra          DECIMAL(10,2) NOT NULL DEFAULT 0,
  margen             DECIMAL(10,2) NOT NULL DEFAULT 0,
  precio_calculado   DECIMAL(10,2) NOT NULL DEFAULT 0,
  precio_manual      DECIMAL(10,2) NULL,
  precio_final       DECIMAL(10,2) NOT NULL DEFAULT 0,
  creado_en          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_encargo_referencia UNIQUE (anio, numero),
  CONSTRAINT fk_encargo_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
  CONSTRAINT ck_encargo_tipo CHECK (tipo IN ('VENTA', 'REGALO')),
  CONSTRAINT ck_encargo_estado CHECK (estado IN ('PRESUPUESTO', 'ACEPTADO', 'EN_COLA', 'IMPRIMIENDO',
                                                 'TERMINADO', 'ENTREGADO', 'COBRADO', 'CANCELADO')),
  INDEX idx_encargo_estado (estado),
  INDEX idx_encargo_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cantidades por unidad: horas, gramos y varios se multiplican por la cantidad de la línea.
CREATE TABLE encargo_linea (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  encargo_id        BIGINT        NOT NULL,
  posicion          INT           NOT NULL,
  modelo_id         BIGINT        NOT NULL,
  cantidad          INT           NOT NULL,
  horas_impresion   DECIMAL(6,2)  NULL,
  horas_mano_obra   DECIMAL(5,2)  NULL,
  varios_concepto   VARCHAR(150)  NULL,
  varios_importe    DECIMAL(8,2)  NULL,
  coste_unitario    DECIMAL(10,2) NOT NULL DEFAULT 0,
  precio_unitario   DECIMAL(10,2) NOT NULL DEFAULT 0,
  importe           DECIMAL(10,2) NOT NULL DEFAULT 0,
  CONSTRAINT fk_linea_encargo FOREIGN KEY (encargo_id) REFERENCES encargo(id) ON DELETE CASCADE,
  CONSTRAINT fk_linea_modelo FOREIGN KEY (modelo_id) REFERENCES modelo(id),
  CONSTRAINT ck_linea_cantidad CHECK (cantidad > 0),
  INDEX idx_linea_modelo (modelo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE linea_filamento (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  linea_id   BIGINT       NOT NULL,
  material   VARCHAR(20)  NOT NULL,
  color      VARCHAR(40)  NULL,
  gramos     DECIMAL(7,2) NOT NULL,
  precio_kg  DECIMAL(6,2) NOT NULL,
  CONSTRAINT fk_filamento_linea FOREIGN KEY (linea_id) REFERENCES encargo_linea(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------- paso de los regalos existentes
-- Mismos id que el regalo original. Sin amortización ni mano de obra ni margen: el coste queda igual.
INSERT INTO encargo (id, anio, numero, cliente_id, tipo, estado, fecha, fecha_entrega, ocasion, notas,
                     incluir_mano_obra, incluir_margen, potencia_w, precio_kwh, mano_obra_hora,
                     amortizacion_hora, margen_pct, coste, mano_obra, margen, precio_calculado,
                     precio_manual, precio_final)
SELECT r.id,
       YEAR(r.fecha),
       ROW_NUMBER() OVER (PARTITION BY YEAR(r.fecha) ORDER BY r.fecha, r.id),
       r.persona_id,
       'REGALO',
       'ENTREGADO',
       r.fecha,
       r.fecha,
       r.ocasion,
       r.notas,
       FALSE,
       FALSE,
       COALESCE(r.potencia_w, 150.0),
       COALESCE(r.precio_kwh, 0.1500),
       12.00,
       0.00,
       30.00,
       v.coste_calculado,
       0,
       0,
       v.coste_calculado,
       r.coste_manual,
       v.coste_total
FROM regalo r
JOIN v_regalo_coste v ON v.id = r.id;

INSERT INTO encargo_linea (id, encargo_id, posicion, modelo_id, cantidad, horas_impresion, horas_mano_obra,
                           varios_concepto, varios_importe, coste_unitario, precio_unitario, importe)
SELECT r.id, r.id, 1, r.modelo_id, 1, r.horas_impresion, NULL,
       r.varios_concepto, r.varios_importe, v.coste_calculado, v.coste_calculado, v.coste_calculado
FROM regalo r
JOIN v_regalo_coste v ON v.id = r.id;

INSERT INTO linea_filamento (id, linea_id, material, color, gramos, precio_kg)
SELECT f.id, f.regalo_id, f.material, f.color, f.gramos, f.precio_kg
FROM regalo_filamento f;

-- ---------------------------------------------------------------- tablas antiguas, apartadas
DROP VIEW v_regalo_coste;

ALTER TABLE regalo
  DROP FOREIGN KEY fk_regalo_persona,
  DROP FOREIGN KEY fk_regalo_modelo;

RENAME TABLE regalo TO legacy_regalo,
             regalo_filamento TO legacy_regalo_filamento;
