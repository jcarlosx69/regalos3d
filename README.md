# Regalos 3D

Gestión interna de un pequeño taller de impresión 3D por encargo: ventas a clientes y regalos, desde el presupuesto hasta la entrega y el cobro, con el coste y el precio de venta de cada encargo.

Los ficheros de cada objeto (STL, 3MF, `.FCStd` de FreeCAD, STEP) y sus fotos viven en **Manyfold**. Esta app guarda clientes, encargos, artículos, bobinas y tarifas, y enlaza cada modelo con Manyfold por su identificador público.

| Pieza | Responsabilidad |
| --- | --- |
| Manyfold (`192.168.1.89:3214`) | Ficheros, fotos, etiquetas, creador y licencia |
| Regalos 3D (esta app) | Clientes (clave: móvil), encargos con varios artículos, estados, coste, precio de venta, aviso de regalo repetido e informes |

**Backend:** Java 21, Spring Boot 3.5, Maven, Spring Data JPA, Flyway, MariaDB 10.11, Spring Security, Testcontainers.
**Frontal** (`frontend/`): Angular 22 (componentes standalone, signals, sin zone.js), Angular Material, formularios reactivos tipados, Vitest.

## Estado: fases 1 y 2 del modelo comercial

- **Encargos** de tipo venta o regalo, con referencia anual `E-2026-0001` y **varios artículos**. Cada artículo lleva modelo, cantidad, horas de impresión y de mano de obra, bobinas (multicolor) y gastos varios (iluminación, mecanismos…); todo por unidad.
- **Estados:** Presupuesto → Aceptado → En cola → Imprimiendo → Terminado → Entregado → Cobrado, más Cancelado. Los regalos terminan en Entregado. Las fechas de entrega y cobro se rellenan solas. Solo se eliminan presupuestos y cancelados.
- **Precio** (`CalculadoraPrecio`, por unidad y redondeando cada componente a céntimos):
  coste = material + energía + amortización + varios · mano de obra = h × €/h · margen = (coste + mano de obra) × % · precio = coste + mano de obra + margen.
  Cada encargo decide si cobra mano de obra y si aplica margen (una venta los incluye; un regalo, no, salvo que se marque). El precio final se puede fijar a mano; el calculado se conserva.
  Cada artículo puede llevar su propio precio por unidad a mano, o ir como **obsequio** (regalo o promoción dentro de una venta): no se cobra, pero su coste cuenta en el beneficio (migración V5).
- **Ajustes:** tarifas del taller (bobina €/kg, potencia, €/kWh, mano de obra €/h, amortización €/h, margen %) editables desde la app. Cada encargo copia las tarifas al crearse; cambiarlas no altera encargos anteriores.
- **Documentos PDF** (fase 2), generados al pedirlos con los datos actuales del encargo y abiertos en otra pestaña:
  - *Presupuesto* (solo ventas, A4): cliente, fecha, validez, artículos con materiales y extras, obsequios, descuento si el precio final se fijó a mano, total y texto al pie.
  - *Albarán* (A4): valorado en ventas y sin precios en regalos; con ocasión, firma de recibido y texto al pie.
  - *Etiqueta del paquete* (A6): destinatario, dirección y contenido; sin precios ni teléfono.
  La cabecera lleva solo la marca. Las notas internas nunca salen. Validez y texto al pie se cambian en Ajustes (migración V6).
  Plantillas Thymeleaf en `src/main/resources/pdf/plantillas` (se pueden abrir en el navegador con datos de ejemplo), convertidas con jsoup + openhtmltopdf; CSS 2.1, sin flexbox ni grid. Fuente Archivo (OFL) incluida.
- **Regalo repetido:** en un regalo, si el cliente ya recibió alguno de los modelos (en cualquier encargo no cancelado), `409 REGALO_DUPLICADO`; se guarda con `?confirmarDuplicado=true`. En ventas no se avisa.
- **Clientes:** nombre y móvil obligatorios (móvil español normalizado a `+34XXXXXXXXX` y único); email y dirección de entrega opcionales.
- **Informes:** facturado, cobrado, pendiente de cobro, beneficio, coste en regalos, trabajo en curso y presupuestos abiertos; por cliente, año, ocasión y modelos más encargados.
- Acceso con usuario y contraseña (hash bcrypt): sesión con cookie y protección CSRF para el frontal; HTTP Basic para `curl` y tests.

### Paso de los datos anteriores (migración V4)

Los regalos registrados con la versión anterior pasan a ser encargos de tipo regalo en estado Entregado, con el mismo coste y precio final, sin amortización ni mano de obra ni margen. `persona` pasa a llamarse `cliente`. Las tablas antiguas **no se borran**: quedan como `legacy_regalo` y `legacy_regalo_filamento` para poder comprobar el paso; se eliminarán en una migración posterior.

## Desarrollo en el portátil

Requisitos: Java 21+, Maven, Podman o Docker y **Node 24** (Angular 22 pide Node ≥ 22.22.3 o ≥ 24.15).

```bash
# 1. MariaDB de desarrollo en 127.0.0.1:3307 (pod de Podman o docker-compose.dev.yml)

# 2. Backend con datos de prueba inventados (usuario admin / contraseña admin-dev).
#    db/dev/afterMigrate.sql los carga tras cada migración sin duplicarlos.
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontal, en otra terminal (la primera vez: npm ci)
cd frontend
npm start
```

Abre `http://localhost:4200`. `ng serve` reenvía `/api` al backend del 8085 (`proxy.conf.json`), así que navegador, cookies y CSRF funcionan igual que en producción, donde Spring Boot sirve el frontal desde el mismo origen.

Para probar el conjunto como en el servidor sin Docker: `cd frontend && npm run build:spring` copia el frontal compilado a `src/main/resources/static/` y `mvn spring-boot:run` lo sirve en `http://localhost:8085`.

**Con curl:** las lecturas funcionan con `curl -u admin:admin-dev …`. Las escrituras necesitan además el token CSRF, así que lo cómodo es usar el frontal o `requests.http` solo para consultas.

### Tests

```bash
mvn verify
```

- `TelefonosTest`, `CalculadoraPrecioTest`, `EncargoServiceTest` y `FormatoTest`: unitarios, no necesitan nada.
- `MotorPdfTest`: genera presupuesto, albaranes y etiqueta de verdad y lee su texto con PDFBox (tamaño A6, regalo sin precios, datos escapados, paginación). No necesita base de datos.
- `RegalosIntegrationTest`: levanta una MariaDB 10.11 real con Testcontainers y prueba migraciones V1-V6, validación del esquema, encargos con varias líneas, estados, regalo repetido, ajustes, documentos PDF, informes, login por sesión, CSRF y rutas del frontal. **Necesita Docker o el socket de Podman**; si no lo hay, se salta sola.

Con Podman, una vez:

```bash
systemctl --user enable --now podman.socket
cat >> ~/.testcontainers.properties <<EOT
docker.host=unix://$XDG_RUNTIME_DIR/podman/podman.sock
ryuk.disabled=true
EOT
```

Frontal: `cd frontend && npx ng test --watch=false` (cálculo de precios con los mismos casos que el test de Java, estados, formulario de encargo, formatos y colores de bobina).

## API

Todas las rutas bajo `/api` requieren sesión (o HTTP Basic), y las que cambian datos, la cabecera `X-XSRF-TOKEN` con el valor de la cookie `REGALOS3D-XSRF-TOKEN`. Errores en formato ProblemDetail (RFC 9457).

| Método | Ruta | Qué hace |
| --- | --- | --- |
| POST | `/api/auth/login` | Formulario `username` + `password`; 204 y cookie de sesión, o 401 |
| POST | `/api/auth/logout` | Cierra la sesión (204) |
| GET | `/api/auth/yo` | Usuario de la sesión, o 401 |
| GET | `/api/clientes?q=` · `/api/clientes/{id}` · `/api/clientes/telefono/{tel}` | Lista, busca, uno |
| POST · PUT · DELETE | `/api/clientes[/{id}]` | Alta, edición, borrado (no si tiene encargos) |
| GET | `/api/modelos?q=` · `/api/modelos/manyfold/{id}` | Lista, busca, o por ID de Manyfold |
| POST · PUT · DELETE | `/api/modelos[/{id}]` | Alta, edición, borrado (no si figura en encargos) |
| GET | `/api/encargos?clienteId=` | Resumen de encargos, del más reciente al más antiguo |
| GET | `/api/encargos/{id}` | Encargo con artículos, bobinas, desglose por unidad, tarifas e importes |
| GET | `/api/encargos/comprobar?clienteId=&modeloIds=1,2&excluir=` | ¿El cliente ya recibió alguno de estos modelos? |
| POST · PUT | `/api/encargos[/{id}]?confirmarDuplicado=` | Alta y edición; `409 REGALO_DUPLICADO` si es un regalo repetido |
| PATCH | `/api/encargos/{id}/estado` | Cambio de estado `{"estado": "ENTREGADO"}` |
| DELETE | `/api/encargos/{id}` | Solo presupuestos y cancelados |
| GET · PUT | `/api/ajustes` | Tarifas del taller |
| GET · PUT | `/api/ajustes/documentos` | Validez del presupuesto y texto al pie |
| GET | `/api/encargos/{id}/presupuesto.pdf` | Presupuesto (solo ventas; en un regalo, 409) |
| GET | `/api/encargos/{id}/albaran.pdf` | Albarán |
| GET | `/api/encargos/{id}/etiqueta.pdf` | Etiqueta A6 del paquete |
| GET | `/api/informes/resumen?anio=` | Facturado, cobrado, pendiente, beneficio, regalos, en curso, presupuestos |
| GET | `/api/informes/por-cliente?anio=` · `por-anio` · `por-ocasion?anio=` | Importes agrupados |
| GET | `/api/informes/modelos?limite=10` | Modelos con más unidades encargadas |
| GET | `/actuator/health` | Estado (público, sin detalles) |

Respuesta de regalo repetido:

```json
{
  "status": 409,
  "title": "Regalo repetido",
  "detail": "Lucía ya recibió Cute rabbit (último: E-2026-0001 del 12/03/2026). Repite con confirmarDuplicado=true para guardarlo igualmente.",
  "codigo": "REGALO_DUPLICADO",
  "previos": [{ "encargoId": 1, "referencia": "E-2026-0001", "fecha": "2026-03-12", "tipo": "REGALO",
                "ocasion": "Cumpleaños", "modeloId": 1, "modelo": "Cute rabbit" }]
}
```

## Cómo guardar un objeto (flujo de trabajo)

1. **En Manyfold**, sube a la biblioteca `Regalos` el `.FCStd` (si es diseño propio), el STL o 3MF, opcionalmente el STEP y una foto de la pieza impresa. Etiqueta `diseño-propio` o `descargado` (y rellena creador y licencia en los descargados).
2. Abre el modelo y copia su identificador de la URL.
3. **En la app**, da de alta el modelo en Modelos con ese ID (o pega la dirección entera del modelo).
4. Al venderlo o regalarlo, crea un encargo con el cliente y uno o varios artículos, y avanza su estado hasta Entregado (y Cobrado, si es una venta).

> Pendiente: confirmar en tu instancia el formato del identificador y que la ruta del modelo es `/models/{id}`. Si no lo es, ajusta `ModeloResponse.de` y, si el ID es más largo de 64 caracteres, la columna `manyfold_model_id` con una migración nueva.

## Uso real en el portátil (modo elegido)

La app se usa solo desde el portátil: no se despliega en el servidor y los datos de terceros no salen del equipo. El uso real va aparte del desarrollo: la app es un `.jar` y solo la base de datos va en un contenedor de Podman.

| | Desarrollo | Uso real |
|---|---|---|
| Base de datos | `127.0.0.1:3307` (pod de desarrollo) | `127.0.0.1:3308`, contenedor `regalos3d-db`, volumen `regalos3d-datos` |
| App | `ng serve` (4200) + `mvn spring-boot:run` (8085) | `~/Apps/regalos3d/regalos3d.jar` en `http://localhost:8086` |
| Datos | inventados (`db/dev`) | los tuyos |

Todo está en `deploy/portatil`:

| Script | Qué hace | Cuándo |
|---|---|---|
| `crear-base.sh` | Crea el contenedor MariaDB real con su volumen | Una vez |
| `construir.sh` | Compila el frontal dentro del backend y deja el `.jar` en `~/Apps/regalos3d` | Cada vez que cambie el código |
| `arrancar.sh` | Arranca la base si estaba parada y lanza la app con tu usuario (Ctrl+C para parar) | Para usarla |
| `copia.sh` | Vuelca la base a `~/Copias/regalos3d` y conserva las 30 últimas | De vez en cuando, y siempre antes de actualizar |

**Primera vez:**

```bash
cd deploy/portatil
cp env.example .env     # contraseñas de la base y hash bcrypt de tu usuario (ver el propio archivo)
./crear-base.sh
./construir.sh
./arrancar.sh           # la primera vez Flyway crea las tablas (V1-V6); abre http://localhost:8086
```

**Actualizar a una versión nueva:** `./copia.sh`, `./construir.sh` y `./arrancar.sh`. Las migraciones nuevas se aplican solas al arrancar.

**Restaurar una copia:** la orden está al principio de `copia.sh`.

**Cuidado:** `podman volume rm regalos3d-datos` (o `podman system reset`) borra todos los datos reales. Conviene guardar de vez en cuando alguna copia fuera del portátil, por ejemplo en el HDD del servidor con permisos solo de root.

## Despliegue en el servidor (alternativa, no usada)

Se descartó para no cargar el servidor ni tocar UFW; se deja documentado por si cambia la decisión. Para esta vía se usan el `Dockerfile` y `deploy/omv-compose.yml`.

Respeta las restricciones del servidor: solo LAN, nada de Cloudflare Tunnel, límites de recursos y verificar labjc tras cada paso.

1. **Puerto libre:** `sudo ss -tlnp | grep 8085` debe salir vacío.
2. **Imagen:** copia el proyecto al servidor y `sudo docker build -t regalos3d:0.1.0 .` El Dockerfile compila primero el frontal con Node y lo mete en el jar, así que en el servidor no hace falta instalar Node.
3. **Hash de la contraseña:**
   ```bash
   htpasswd -nbBC 12 "" 'tu-contraseña-robusta' | tr -d ':\n'; echo
   ```
   (paquete `apache2-utils`; Spring acepta el prefijo `$2y$`). Va entre comillas simples en el `.env`.
4. **Base de datos**, elige una:
   - **A — `regalos_db` en la MariaDB del host** (toca zona protegida, requiere confirmación). Usuario propio solo para esa base. El contenedor conecta a `192.168.1.89:3306`, y su tráfico llega desde la red de Docker (`172.x`), no desde la LAN: hará falta un usuario `regalos@'172.%'` y revisar si UFW deja pasar esa subred al 3306.
     ```sql
     CREATE DATABASE regalos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
     CREATE USER 'regalos'@'172.%' IDENTIFIED BY '...';
     GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES, CREATE VIEW ON regalos_db.* TO 'regalos'@'172.%';
     ```
   - **B — contenedor MariaDB propio:** descomenta el servicio `db` en `deploy/omv-compose.yml`. Aislamiento total de labjc, unos 100-150 MB más de RAM.
5. **Stack en OMV** (Servicios → Compose → Archivos): pega `deploy/omv-compose.yml` y, en su `.env`, el contenido de `deploy/env.example` con tus valores.
6. **Comprobar:** `curl -u carlos:... http://192.168.1.89:8085/api/informes/resumen`, y después labjc: `systemctl is-active labjc.service apache2` y `curl -sI https://labjc.es` → `HTTP/2 200`.
7. **Copia de seguridad** del volcado de `regalos_db` al HDD, con permisos solo de root, siguiendo el modelo de `backup_labjc_db.sh`.

## Privacidad

La app guarda nombres y móviles de terceros: no salen de casa.

- Solo LAN, nunca por el túnel ni por redirección de puertos.
- Los teléfonos no se guardan en Manyfold.
- En el repositorio solo hay datos inventados (`db/dev`); `.env` y credenciales están en `.gitignore`.

## Siguientes pasos

- Eliminar las tablas `legacy_*` una vez comprobado el paso de datos.
