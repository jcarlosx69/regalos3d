-- Esquema inicial acordado el 05/10/2026.
-- El coste lo calcula MariaDB con columnas generadas: cada regalo guarda los datos del momento.

CREATE TABLE persona (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  telefono  VARCHAR(16)  NOT NULL UNIQUE,
  nombre    VARCHAR(100) NOT NULL,
  notas     VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE modelo (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  manyfold_model_id  VARCHAR(64)  NOT NULL UNIQUE,
  nombre             VARCHAR(150) NOT NULL,
  gramos_estimados   DECIMAL(7,2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE regalo (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  persona_id       BIGINT NOT NULL,
  modelo_id        BIGINT NOT NULL,
  fecha            DATE   NOT NULL,
  ocasion          VARCHAR(100),
  notas            VARCHAR(500),
  horas_impresion  DECIMAL(5,2),
  potencia_w       DECIMAL(6,1),
  precio_kwh       DECIMAL(6,4),
  coste_luz        DECIMAL(8,2) AS (horas_impresion * potencia_w / 1000 * precio_kwh) STORED,
  CONSTRAINT fk_regalo_persona FOREIGN KEY (persona_id) REFERENCES persona(id),
  CONSTRAINT fk_regalo_modelo  FOREIGN KEY (modelo_id)  REFERENCES modelo(id),
  INDEX idx_persona_modelo (persona_id, modelo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE regalo_filamento (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  regalo_id  BIGINT       NOT NULL,
  material   VARCHAR(20)  NOT NULL,
  color      VARCHAR(40),
  gramos     DECIMAL(7,2) NOT NULL,
  precio_kg  DECIMAL(6,2) NOT NULL,
  coste      DECIMAL(8,2) AS (gramos * precio_kg / 1000) STORED,
  CONSTRAINT fk_filamento_regalo FOREIGN KEY (regalo_id) REFERENCES regalo(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
