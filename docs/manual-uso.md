# Regalos 3D — Manual de uso

Oct 6, 2026 · @Juan Carlos

## Arrancar y parar

Regalos 3D se abre en `http://localhost:8086` tras ejecutar un script; solo funciona desde el propio portátil.

Registra clientes, encargos de venta o regalo con su coste y precio, y genera presupuestos, albaranes y etiquetas en PDF. Los archivos 3D siguen en Manyfold.

```
cd ~/Proyectos/regalos3d/deploy/portatil
./arrancar.sh
```

- Espera a ver `Started Regalos3dApplication` en la terminal y abre `http://localhost:8086`.
- Si la base de datos estaba parada (por ejemplo, tras reiniciar el portátil), el script la arranca solo.
- Para parar la app: `Ctrl+C` en esa terminal. La base sigue en marcha y no pasa nada.
- La sesión dura 8 horas; para salir antes, el botón de cierre de sesión junto a tu usuario, abajo a la izquierda.

## Primeros pasos

Antes del primer encargo, revisa las tarifas en Ajustes: cada encargo nuevo las copia y después ya no cambian solas.

1. **Entra** con tu usuario y contraseña.
2. **Ajustes:** revisa cada grupo.
   - Material: precio de la bobina de 1 kg (20 € por defecto).
   - Energía: potencia media de la impresora (W) y precio de la luz (€/kWh).
   - Taller: mano de obra (€/h) y amortización de la impresora (€/h de impresión).
   - Venta: margen de beneficio (%).
   - Documentos: días de validez del presupuesto y texto al pie (condiciones, plazos, forma de pago).
3. **Modelos:** da de alta los modelos que imprimes con su nombre, el ID público de Manyfold (el de la URL `/models/ID`) y los gramos estimados. El peso rellena solo la primera bobina al elegir el modelo en un encargo.
4. **Clientes:** nombre y móvil obligatorios; email y dirección de entrega opcionales. El móvil se guarda como +34 y no se puede repetir.

Cambiar las tarifas no altera los encargos ya creados; si quieres aplicarlas a uno, ábrelo y usa "Usar las tarifas actuales de Ajustes".

## Encargos

Un encargo agrupa uno o varios artículos para un cliente, como venta o como regalo, y se sigue desde el presupuesto hasta la entrega y el cobro. Se crea con el botón **Nuevo encargo**.

### Crear un encargo

1. **Venta o Regalo:** una venta cobra mano de obra y margen; un regalo se calcula al coste. Puedes cambiarlo después con las casillas de "Cálculo del precio".
2. **Cliente:** búscalo por nombre o móvil, o créalo con **Nuevo** sin salir del formulario. Añade fecha y, si quieres, la ocasión.
3. **Artículos:** por cada uno, el modelo (o **Nuevo** para darlo de alta), la cantidad y, por unidad:
   - horas de impresión y de mano de obra;
   - una bobina por cada material o color, con sus gramos y su €/kg (**Añadir bobina** para multicolor);
   - varios: iluminación, mecanismos, imanes, con su importe.
4. **Precio por artículo:** vacío usa el calculado; escribe otro para fijarlo a mano.
5. **Obsequio sin cargo** (solo en ventas): el artículo va a 0 €, pero su coste cuenta en el beneficio.
6. **Notas internas:** escala, perfil de laminado, plazo acordado. Nunca salen en los documentos.
7. **Guardar encargo.** Recibe la referencia `E-AAAA-NNNN`.

El panel **Presupuesto** de la derecha recalcula en vivo: artículos, coste de fabricación, mano de obra, margen, obsequios y precios a mano, y el total con el beneficio. En **Precio final** puedes fijar el total a mano (por ejemplo, redondear); "Usar el calculado" lo deshace.

### Regalo repetido

En un regalo, si el cliente ya recibió alguno de los modelos en otro encargo no cancelado, aparece un aviso con la referencia y la fecha. Puedes guardarlo igualmente confirmándolo.

### Estados

Una venta pasa por Presupuesto, Aceptado, En cola, Imprimiendo, Terminado, Entregado y Cobrado; un regalo empieza en En cola y termina en Entregado. Cualquier encargo se puede cancelar y reactivar.

&#91;embedded content: estados de un encargo · 7 pasos y Cancelado\]

- Cambia el estado con los pasos de arriba del formulario y guarda, o con el botón de avance de cada fila en la lista.
- La fecha de entrega y la de cobro se rellenan solas al llegar a esos estados.
- Solo se pueden eliminar presupuestos y encargos cancelados.

### Lista de encargos

Pestañas Activos, Por cobrar, Finalizados, Cancelados y Todos, con buscador por referencia, cliente, modelo u ocasión y filtro por tipo. El menú ⋮ de cada fila abre, cancela, elimina o descarga sus documentos.

## Documentos PDF

Los documentos se abren desde el botón **Documentos** de un encargo guardado, o desde el menú ⋮ de su fila, en una pestaña nueva lista para imprimir o guardar.

| Documento | Formato | Cuándo | Qué lleva |
| --- | --- | --- | --- |
| Presupuesto | A4 | Solo ventas | Cliente, fecha, válido hasta, artículos con materiales y extras, obsequios, descuento si fijaste el precio final, total y texto al pie |
| Albarán | A4 | Ventas y regalos | Destinatario, fecha de entrega, ocasión, artículos y firma de recibido; con precios en ventas y sin precios en regalos |
| Etiqueta del paquete | A6 (105 × 148 mm) | Ventas y regalos | Destinatario, dirección y contenido; sin precios ni teléfono |

- Se generan al pedirlos con los datos guardados: si tienes cambios sin guardar, el menú lo avisa; guarda primero.
- La validez del presupuesto y el texto al pie se cambian en **Ajustes → Documentos**.
- Las notas internas del encargo nunca aparecen en ningún documento.
- Para imprimir la etiqueta, elige tamaño A6 (o "ajustar a la página" sobre A4) en el diálogo de impresión.

## Clientes, modelos e informes

**Clientes.** La lista busca por nombre, móvil o email. La ficha de cada cliente muestra sus encargos y desde ella se crea uno nuevo con el cliente ya elegido. Un cliente con encargos no se puede eliminar.

**Modelos.** Cada modelo enlaza con su ficha en Manyfold (`http://192.168.1.89:3214/models/ID`), donde están los STL, 3MF, FreeCAD y las fotos. El enlace solo funciona en casa, dentro de la red local. En Manyfold nunca se guardan teléfonos.

**Informes.** Se filtran por año y muestran:

- Ventas: facturado, cobrado, pendiente de cobro y beneficio (solo encargos entregados o cobrados).
- Regalos: número, lo que te costaron y su valor.
- Trabajo en curso y presupuestos abiertos, con sus importes.
- Desglose por cliente, por año, por ocasión y modelos más encargados.

## Copias de seguridad y actualizaciones

Tus datos viven en el volumen de Podman `regalos3d-datos`; haz una copia de vez en cuando y siempre antes de actualizar la app.

**Hacer una copia** (con la base en marcha):

```
cd ~/Proyectos/regalos3d/deploy/portatil
./copia.sh
```

Guarda un volcado comprimido en `~/Copias/regalos3d`, legible solo por tu usuario, y conserva los 30 más recientes. Lleva de vez en cuando alguna copia fuera del portátil (por ejemplo, al HDD del servidor con permisos solo de root).

**Restaurar una copia** (sustituye los datos actuales; para antes la app):

```
gunzip -c ~/Copias/regalos3d/ARCHIVO.sql.gz | podman exec -i regalos3d-db sh -c 'exec mariadb -uroot -p"$MARIADB_ROOT_PASSWORD" regalos_db'
```

**Actualizar a una versión nueva del código:**

1. `./copia.sh`
2. `./construir.sh` — compila frontal y backend y deja el nuevo `~/Apps/regalos3d/regalos3d.jar`.
3. `./arrancar.sh` — las migraciones nuevas se aplican solas al arrancar.

**Nunca** ejecutes `podman volume rm regalos3d-datos` ni `podman system reset`: borran todos los datos reales.

## Problemas frecuentes

| Síntoma | Qué hacer |
| --- | --- |
| `arrancar.sh` dice que falta el `.jar` | Ejecuta `./construir.sh` |
| `arrancar.sh` dice que no existe la base | Ejecuta `./crear-base.sh` (solo la primera vez) |
| El script avisa de valores `CAMBIAR_` | Rellena el `.env` de `deploy/portatil` |
| No puedo entrar con mi contraseña | Genera un hash nuevo con `htpasswd -nbBC 12 "" 'contraseña' \| tr -d ':\n'`, ponlo entre comillas simples en `ADMIN_PASSWORD_HASH` y vuelve a arrancar |
| "Port 8086 already in use" | La app ya está abierta en otra terminal: usa esa o párala con `Ctrl+C` |
| El enlace a Manyfold no abre | Solo funciona en casa, dentro de la red local |
| El PDF no refleja un cambio | Guarda el encargo y vuelve a abrir el documento |
| No se puede eliminar un encargo | Solo se eliminan presupuestos y cancelados: cancélalo antes |

**Referencia rápida** (desde `~/Proyectos/regalos3d/deploy/portatil`):

| Orden | Para qué |
| --- | --- |
| `./arrancar.sh` | Usar la app en `http://localhost:8086` |
| `./copia.sh` | Copia de seguridad |
| `./construir.sh` | Compilar tras un cambio de código |
| `./crear-base.sh` | Crear la base real (una vez) |
| `podman ps` | Ver si la base `regalos3d-db` está en marcha |
