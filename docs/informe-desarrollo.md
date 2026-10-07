# Regalos 3D — Informe de desarrollo

Oct 6, 2026 · @Juan Carlos

## Resumen

Regalos 3D está terminada en sus fases 1 y 2 y en uso real en el portátil desde el 6 de octubre de 2026. Es una aplicación web de gestión interna para un taller de impresión 3D: registra clientes, encargos (ventas y regalos), su coste y precio, y genera presupuestos, albaranes y etiquetas en PDF.

- **Origen:** una app para recordar qué objeto impreso se regaló a cada persona, con Manyfold como almacén de los archivos 3D.
- **Evolución:** tras un cuestionario de requisitos se convirtió en una herramienta más comercial, con encargos de varias líneas, flujo de estados y cálculo de precio de venta.
- **Estado:** 62 tests de backend y 35 de frontal en verde; funcionando con base de datos real en `http://localhost:8086`.
- **Fuera de alcance:** el catálogo con miniaturas de Manyfold (fase 3) se descartó: la propia web de Manyfold y los enlaces de la app cubren esa necesidad.

## Requisitos y decisiones

Los requisitos salieron de un cuestionario hecho antes de tocar el código de la versión comercial; el desarrollo se planó por fases.

| Tema | Decisión |
| --- | --- |
| Uso | Regalos y ventas; un solo usuario, gestión interna |
| Precio | Coste + mano de obra + amortización + margen; casillas por encargo; regalos al coste por defecto |
| Estados | Flujo completo: Presupuesto, Aceptado, En cola, Imprimiendo, Terminado, Entregado, Cobrado, más Cancelado |
| Artículos | Varias líneas por encargo; precio por artículo editable y obsequios a 0 € |
| Clientes | Nombre y móvil obligatorios; email y dirección opcionales |
| Filamento | Sin inventario; precio de bobina por defecto 20 €/kg |
| Impuestos | Sin IVA ni IGIC |
| Documentos | Presupuesto y albarán en PDF, etiqueta A6; cabecera solo con la marca Regalos 3D |
| Nombres | Encargos, Clientes, Modelos |

**Restricciones del entorno:** el servidor doméstico (Debian 12 + OMV 7) aloja labjc.es en producción, con una zona protegida (labjc, Apache, UFW, base `labjc_db`) que no se toca sin confirmación. Todo servicio va solo por la LAN, nunca por túnel ni redirección de puertos.

**Privacidad:** los nombres y móviles de terceros no salen de casa, los móviles no se guardan en Manyfold, el repositorio solo lleva datos inventados y las credenciales nunca van a Git. El acceso pide usuario y contraseña aunque sea local.

## Arquitectura y tecnologías

Todo corre en un único `.jar` de Spring Boot que sirve el frontal Angular compilado, la API REST y los documentos PDF; los datos van en MariaDB y los archivos 3D siguen en Manyfold.

&#91;embedded content: arquitectura · navegador, app Java, MariaDB y Manyfold\]

El navegador solo habla con la app; Manyfold no se consulta desde el código, la app guarda el ID público de cada modelo y construye el enlace a su ficha.

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.6, Spring Data JPA (Hibernate 6.6), JdbcClient para informes, Bean Validation, ProblemDetail (RFC 9457) |
| Seguridad | Spring Security: login por formulario con sesión, CSRF con cookie para Angular, HTTP Basic para pruebas, contraseña en bcrypt |
| Base de datos | MariaDB 10.11, migraciones Flyway |
| PDF | Plantillas Thymeleaf, jsoup, openhtmltopdf 1.1.87 (PDFBox 3), fuente Archivo (OFL) |
| Frontal | Angular 22 (standalone, signals, zoneless), Angular Material 22, formularios reactivos tipados |
| Pruebas | JUnit 5, Mockito, AssertJ, Testcontainers con Podman, Vitest |
| Ejecución | `.jar` en el portátil (Java 25) y base en contenedor de Podman |

## Modelo de datos y migraciones

El esquema se versiona con Flyway en seis migraciones, y Hibernate solo lo valida (`ddl-auto: validate`). El centro es el encargo: un cliente, varias líneas, cada línea un modelo de Manyfold con sus bobinas.

| Tabla | Qué guarda |
| --- | --- |
| `cliente` | Nombre, móvil (único, normalizado a +34), email, dirección, notas |
| `modelo` | Nombre e ID público del modelo en Manyfold, gramos estimados |
| `encargo` | Referencia anual `E-AAAA-NNNN`, tipo, estado, fechas, tarifas copiadas, costes y precios |
| `encargo_linea` | Cantidad, horas de impresión y de mano de obra, varios, obsequio, precio manual, importes |
| `linea_filamento` | Material, color, gramos y €/kg de cada bobina |
| `ajustes` | Fila única: tarifas del taller, validez del presupuesto y texto al pie |

| Migración | Cambio |
| --- | --- |
| V6 | Ajustes de documentos: validez del presupuesto y texto al pie |
| V5 | Precio por artículo y obsequios en `encargo_linea` |
| V4 | Modelo comercial: `persona` pasa a `cliente`, encargos con líneas, ajustes; los regalos antiguos se migran a encargos y sus tablas quedan como `legacy_*` |
| V3 | Gastos varios y precio final manual |
| V2 | Vista de coste por regalo |
| V1 | Esquema inicial: personas, modelos, regalos |

Decisiones técnicas: los enumerados se guardan como texto con `AttributeConverter`, los booleanos como `BIT(1)` y los enteros como `INT`, para que la validación de Hibernate pase sobre MariaDB. Los datos de prueba (ids 9001+) solo se cargan con el perfil `dev`, mediante un `afterMigrate.sql` idempotente.

## Funcionalidades por fase

La fase 1 convirtió el registro de regalos en gestión de encargos; la fase 2 añadió los documentos; la fase 3 se descartó.

**Fase 1 — modelo comercial**

- Encargos de venta o regalo con varios artículos, bobinas multicolor y gastos varios (iluminación, mecanismos).
- Flujo de estados con botón de avance rápido; las fechas de entrega y cobro se rellenan solas; un regalo no se cobra; solo se eliminan presupuestos y cancelados.
- Cálculo de precio por unidad: material + energía + amortización + varios = coste; más mano de obra y margen opcionales. El mismo cálculo existe en Java y en TypeScript para el presupuesto en vivo.
- Precio final editable a mano; el calculado se conserva.
- Aviso de regalo repetido: si el cliente ya recibió el modelo, la API responde 409 y pide confirmación.
- Pantalla de Ajustes con las tarifas del taller; cada encargo copia las suyas al crearse.
- Informes: facturado, cobrado, pendiente, beneficio, coste de regalos, trabajo en curso; por cliente, año, ocasión y modelos.
- Migración de los regalos anteriores a encargos sin pérdida de datos.

**Ampliación — precio por artículo y obsequios**

- Cada artículo admite un precio por unidad manual.
- Un artículo puede ir como obsequio (regalo o promoción dentro de una venta): se cobra a 0 €, pero su coste cuenta en el beneficio.

**Fase 2 — documentos PDF**

- Presupuesto A4 (solo ventas) con validez, obsequios, descuento si el precio final se fijó a mano y texto al pie.
- Albarán A4 valorado en ventas y sin precios en regalos, con ocasión y espacio de firma.
- Etiqueta A6 para el paquete, sin precios ni teléfono.
- Menú Documentos en cada encargo y en la lista; las notas internas nunca salen en los documentos.

**Fase 3 — descartada:** catálogo con miniaturas de Manyfold. La lista de Modelos ya enlaza cada modelo con su ficha en Manyfold.

## Pruebas y calidad

Al cierre pasan 62 tests de backend y 35 de frontal. Los cálculos de precio usan los mismos casos en Java y en TypeScript, así que ambos lados no pueden divergir sin que falle un test.

| Suite | Tipo | Qué cubre |
| --- | --- | --- |
| `CalculadoraPrecioTest` | Unitario | Desglose por unidad, totales, precio manual, obsequios, redondeo a céntimos |
| `EncargoServiceTest` | Unitario (Mockito, reloj fijo) | Numeración anual, estado inicial, tarifas por defecto, regalo repetido |
| `TelefonosTest`, `FormatoTest` | Unitario | Normalización de móviles; euros, fechas y teléfonos en los PDF |
| `MotorPdfTest` | Unitario | Genera los PDF reales y lee su texto con PDFBox: obsequios, regalo sin precios, tamaño A6, escapado, paginación |
| `RegalosIntegrationTest` | Integración (Testcontainers, MariaDB 10.11) | Migraciones V1–V6, validación del esquema, encargos, estados, informes, ajustes, PDF, login, CSRF, rutas de Angular |
| Specs de Angular (Vitest) | Unitario | Cálculo, estados, formulario de encargo, formatos, colores de bobina |

Verificaciones adicionales durante el desarrollo:

- Migración V4 probada sobre una MariaDB 10.11 real con datos antiguos: costes migrados idénticos.
- Recorridos completos en navegador (Playwright) contra un servidor simulado, en escritorio y móvil, sin errores de consola.
- Plantillas PDF previsualizadas en Chromium antes de tener las librerías Java, con un intérprete mínimo de Thymeleaf.

## Incidencias y soluciones

La mayoría de los problemas vinieron del entorno (puertos, Podman, archivos que no llegaron al proyecto), no del código.

| Incidencia | Causa | Solución |
| --- | --- | --- |
| Error `caching_sha2_password` al conectar | Respondía un MySQL en el 3307, no MariaDB | Pod de Podman con `mariadb:10.11` en `127.0.0.1:3307` |
| Conexión rechazada | Perfil mal escrito y puerto 3306 en `application-dev.yml` | Perfil `dev` y puerto 3307 |
| Testcontainers no encuentra Docker | Faltaba el socket de Podman | `podman.socket` activo y `~/.testcontainers.properties` |
| No compila tras descomprimir la fase 1 | Quedaban paquetes antiguos (`regalo`, `persona`) | Borrarlos y `mvn clean verify` |
| Falla el test de la cookie CSRF | `csrf()` de spring-security-test cambia el repositorio del filtro compartido | `@DirtiesContext` en ese test |
| Flyway no arranca en dev | La semilla antigua V1000 contaba como "future", no "missing" | `ignore-migration-patterns: "*:missing,*:future"` |
| Tests de PDF y de documentos | Letter-spacing en el texto extraído; regalo repetido a propósito | Ajustar las aserciones y confirmar el duplicado |
| No se construye la imagen | Faltaban `Dockerfile`, `package-lock.json`, `angular.json` y paquetes Java en la carpeta | Se entregó el proyecto completo y se cambió a `.jar` + base en contenedor |

Lección principal: descomprimir entregas parciales encima del proyecto deja la carpeta en un estado difícil de ver. Con Git, `git status` lo habría mostrado al momento.

## Despliegue y pendientes

La app se usa solo en el portátil: un `.jar` en `~/Apps/regalos3d` contra una MariaDB 10.11 en un contenedor de Podman (`127.0.0.1:3308`, volumen `regalos3d-datos`), con scripts en `deploy/portatil`.

Se descartó desplegar en el servidor por tres motivos: la memoria va justa (swap casi llena), conectar el contenedor con la MariaDB del host obligaba a añadir una regla de UFW (zona protegida) y la app solo se usa desde el portátil. El `Dockerfile` y `deploy/omv-compose.yml` quedan documentados por si cambia la decisión.

- [ ] Iniciar Git en el proyecto y hacer el primer commit
- [ ] Borrar `~/Proyectos/regalos3d-incompleto` tras comprobar que no falta nada
- [ ] Copias periódicas con `copia.sh` y alguna fuera del portátil
- [ ] Migración que elimine las tablas `legacy_*`
- [ ] Quitar la imagen de prueba `busybox` del servidor
