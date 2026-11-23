# VAlqui — Backend (Java 17 + Spring Boot + MongoDB)

API REST de VAlqui, la plataforma de alquiler de vehículos. Implementa las vistas de arquitectura de `docs/VAlqui/` (contexto, funcional, lógica, desarrollo y despliegue) y las historias de usuario HU-01 a HU-18 (mockups en `docs/mockups/`).

## Alcance

- El entregable es la **API** (`valqui-api.jar`). La "Interfaz web VAlqui" de la vista de despliegue no se construye: está representada por los mockups de `docs/mockups/`.
- En producción, el HTTPS (puerto 443) lo pone el servidor donde se publica la API. En local corre en HTTP por el puerto 8080.
- La pasarela de pago es externa: `PasarelaPagoClient` funciona como simulador.

## Requisitos

| Herramienta | Versión |
|---|---|
| Java (JDK) | 17 |
| Maven | 3.9 o superior |
| MongoDB | 6 o superior en local, o un clúster de MongoDB Atlas |
| Newman (opcional, para las pruebas) | 6.x (`npm i -g newman`) |

## Configuración

`src/main/resources/application.properties`:

| Propiedad | Variable de entorno | Valor por defecto |
|---|---|---|
| `spring.data.mongodb.uri` | `MONGODB_URI` | `mongodb://localhost:27017/valqui` |
| `pasarela.url-api` | `PASARELA_URL_API` | URL simulada de la pasarela |
| `pasarela.api-key` | `PASARELA_API_KEY` | `sk_test_valqui_simulada` |
| `valqui.jwt.secreto` | `JWT_SECRETO` | clave de desarrollo (cámbiala en producción, mínimo 32 caracteres) |
| `server.port` | — | `8080` |

### MongoDB Atlas (producción)

1. Crea un clúster en [MongoDB Atlas](https://www.mongodb.com/atlas) y un usuario de base de datos.
2. En *Network Access*, permite la IP del servidor de aplicación.
3. Copia la cadena de conexión (*Connect → Drivers*). Atlas usa TLS por defecto.
4. Define la variable de entorno antes de arrancar:

```powershell
$env:MONGODB_URI = "mongodb+srv://<usuario>:<contrasena>@<cluster>.mongodb.net/valqui?retryWrites=true&w=majority"
```

## Cómo correrlo

```powershell
mvn spring-boot:run
```

O bien, empaquetado como un solo artefacto (`target/valqui-api.jar`):

```powershell
mvn clean package
java -jar target/valqui-api.jar
```

**Usa una base de datos vacía.** Los documentos creados con versiones anteriores (sucursales y coberturas con id) no son compatibles con este modelo.

Al arrancar con la base de datos vacía, `CargaInicial` crea estos datos semilla:

| Rol | Correo | Contraseña | Detalle |
|---|---|---|---|
| Administrador | `admin@valqui.com` | `Admin123*` | — |
| Compañía | `rentacol@valqui.com` | `Renta123*` | RentaCol, NIT `900123456-7`, Bogotá y Medellín, 24 h de cancelación gratuita, penalización 50000, depósito 300000 |
| Agente de sucursal | `agente.norte@valqui.com` | `Agente123*` | `Sucursal Norte` de RentaCol |

Sucursales semilla de RentaCol: `Sucursal Norte` y `Sucursal Sur`, ambas en Bogotá.

## Colección de Postman

`VAlqui.postman_collection.json` (formato v2.1): 9 carpetas y 60 requests, cada uno con `pm.test`. Las fechas se generan en el pre-request de la colección, y los tokens y códigos se guardan solos en variables de colección.

- **Postman:** *Import* → elige el archivo → *Run collection* (en orden).
- **Newman:**

```powershell
npx newman run VAlqui.postman_collection.json
```

Córrela después de la carga inicial. El NIT, los correos y las placas llevan un sufijo por ejecución, así que también se puede volver a correr sin vaciar la base de datos.

## Arquitectura

```
com.valqui
├── presentacion   → FiltroSesion, UsuarioController, CompaniaController, VehiculoController,
│                    ReservaController, EntregaController, ManejadorErrores
│                    (+ dto/: Respuestas, DetalleVehiculo, ResultadoEntrega)
├── negocio        → UsuarioService, CompaniaService, VehiculoService, ReservaService,
│                    EntregaService, PagoService
├── datos          → UsuarioRepository, CompaniaRepository, VehiculoRepository, ReservaRepository
├── integracion    → IPasarelaPago, PasarelaPagoClient (simulador)
└── modelo         → Usuario {abstract}, Cliente, UsuarioCompania, AgenteSucursal, Administrador,
                     MetodoPago, CompaniaAliada, Sucursal, Cobertura, Vehiculo, Reserva,
                     PenalizacionDano, EstadoVehiculo, EstadoReserva, NegocioException
```

Controller → Service → Repository, con inyección por constructor. Cada controller usa solo su service, y `modelo` no depende de ningún otro paquete del proyecto. Colecciones de MongoDB: `usuarios` (las 4 subclases, con el campo `tipo`), `companias` (con sucursales y coberturas embebidas), `vehiculos` y `reservas`. El `_id` es el NIT en `companias`, la placa en `vehiculos` y el código en `reservas`.

**Dependencia `MetodoPago` «usa» `IPasarelaPago` (vista lógica).** `MetodoPago` no tiene métodos de cobro. La dependencia se cumple porque su `token` es el dato que recibe `IPasarelaPago.procesarCobro`, llamado desde `PagoService`.

Sesión: `POST /api/auth/login` devuelve un JWT con el id, el correo y el tipo del usuario. Todas las rutas, salvo el login y el registro de clientes, exigen `Authorization: Bearer <token>` (401 si falta o es inválido, 403 si el rol no corresponde). Todos los errores responden `{ "mensaje": "...", "codigo": N }`.

## Endpoints

Cómo se envían los datos: **JSON** en el body cuando el diagrama recibe un objeto, **form** (`x-www-form-urlencoded`) cuando recibe valores simples, y **query** en la URL.

| Método y ruta | Rol | HU | Envío | Respuestas |
|---|---|---|---|---|
| POST `/api/usuarios/clientes` | público | HU-01 | JSON: `nombreCompleto, correo, contrasena, fechaNacimiento, numeroContacto, paisResidencia` | 201 · 400 · 409 |
| POST `/api/auth/login` | público | HU-18 | form: `correo, contrasena` | 200 · 400 · 401 |
| POST `/api/clientes/{clienteId}/metodos-pago` | CLIENTE (el mismo) | R03, HU-06 | JSON: `marca, tipo, ultimosDigitos, token` | 201 · 400 · 403 · 409 |
| POST `/api/companias?correoAcceso=...` | ADMIN | HU-15 | query: `correoAcceso` · JSON: `nit, nombre, ciudadesOperacion` | 201 · 400 · 409 |
| POST `/api/companias/{nit}/sucursales` | COMPANIA (su nit) | HU-09 | JSON: `nombre, ciudad, direccion, coordenadas, horarioAtencion` | 201 · 400 · 409 |
| PUT `/api/companias/{nit}/politica` | COMPANIA (su nit) | HU-12 | form: `horasGratis, penalizacion, deposito` | 200 · 400 |
| POST `/api/companias/{nit}/coberturas` | COMPANIA (su nit) | HU-13 | JSON: `nombre, costo` | 201 · 400 · 409 |
| POST `/api/vehiculos` | COMPANIA | HU-10 | JSON: `placa, marca, modelo, categoria, anio, transmision, capacidadPasajeros, tarifaDiaria, kilometrajeIncluido, franquiciaDanos, sucursal` (nombre) | 201 · 400 · 409 |
| GET `/api/vehiculos/disponibles` | CLIENTE | HU-02 | query: `ciudad, desde, hasta, categoria, nit` | 200 · 400 |
| GET `/api/vehiculos/{placa}` | CLIENTE | HU-03, R15 | — | 200 · 404 |
| PUT `/api/vehiculos/{placa}/sucursal` | COMPANIA (dueña) | HU-11 | form: `sucursal` (nombre) | 200 · 400 · 403 · 409 |
| PUT `/api/vehiculos/{placa}/estado` | COMPANIA (dueña) | HU-14 | form: `estado` | 200 · 400 · 403 · 409 |
| POST `/api/reservas` | CLIENTE | HU-04 | JSON: `placa, fechaHoraRecogida, fechaHoraDevolucion, sucursalRecogida, sucursalDevolucion` (nombres) | 201 · 400 · 403 · 404 · 409 |
| POST `/api/reservas/{codigo}/coberturas` | CLIENTE dueño | HU-05 | form: `cobertura` (nombre) | 200 · 400 · 403 · 409 |
| POST `/api/reservas/{codigo}/confirmar` | CLIENTE dueño | HU-06 | form: `ultimosDigitos` | 200 · 400 · 403 · 409 |
| POST `/api/reservas/{codigo}/cancelar` | CLIENTE dueño | HU-07, R12 | — | 200 · 403 · 409 |
| GET `/api/reservas/cliente/{clienteId}` | CLIENTE (el mismo) | HU-08, R20 | — | 200 · 403 |
| POST `/api/entregas/{codigo}/entrega` | AGENTE | HU-16 | form: `documentosValidos` | 200 · 400 · 403 · 404 · 409 |
| POST `/api/entregas/{codigo}/devolucion` | AGENTE | HU-17 | form: `valorDano` (0 o ausente = sin daño), `motivoDano` | 200 · 400 · 403 · 404 · 409 |

Pasarela simulada: aprueba si el token no está vacío y el monto es mayor que 0; rechaza el token `tok_rechazado`.

## Supuestos

- **Cuenta de la compañía (HU-15).** `crearCuentaCompania(nit, correo)` crea el `UsuarioCompania` con contraseña inicial igual al **NIT**, cifrada con BCrypt. `nombreCompleto` es el nombre de la compañía; `cargo` y `telefono` quedan vacíos.
- **Política de cancelación (HU-07).** La reserva no guarda copia: `ReservaService.cancelar` consulta la política vigente de la compañía y se la entrega a `Reserva` en campos `@Transient`. Si hay penalización, se cobra con el método de pago de la confirmación; si el cobro falla, la reserva no se cancela.
- **Identidad.** Sucursales y coberturas se identifican por su nombre dentro de la compañía, y los nombres repetidos responden 409.
- **Datos que las firmas del diagrama no reciben.** `ReservaService.iniciarAlquiler(codigo)` y `finalizarAlquiler` toman al agente de la sesión desde la petición, y `iniciarAlquiler` también toma `documentosValidos`, que deja ahí `EntregaService`. `crearCuentaCompania` toma de la misma forma el nombre de la compañía.
- **Dueño de la reserva.** `ReservaController` lo comprueba con el historial del cliente, así que un código inexistente o ajeno responde 403.
- **Respuesta de entrega y devolución.** `EntregaService` devuelve `void`, por lo que `ResultadoEntrega` contiene el código, los estados resultantes y el daño recibido.
- **Estado RESERVADO.** Solo lo asigna la entrega. `VehiculoController.cambiarEstado` rechaza asignarlo a mano.
