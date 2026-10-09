# API REST

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
  "detail": "Lucía Prueba ya recibió Cute rabbit (último: E-2026-0001 del 12/03/2026). Repite con confirmarDuplicado=true para guardarlo igualmente.",
  "codigo": "REGALO_DUPLICADO",
  "previos": [{ "encargoId": 1, "referencia": "E-2026-0001", "fecha": "2026-03-12", "tipo": "REGALO",
                "ocasion": "Cumpleaños", "modeloId": 1, "modelo": "Cute rabbit" }]
}
```
