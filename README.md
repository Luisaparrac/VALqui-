# VAlqui — Backend (Java 17 + Spring Boot + MongoDB)

API REST de VAlqui, la plataforma de alquiler de vehículos. Implementa las vistas de arquitectura de `docs/arquitectura/` (contexto, funcional, lógica, desarrollo y despliegue) y las historias de usuario HU-01 a HU-18 (mockups en `docs/mockups/`).

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

Al arrancar con la base de datos vacía, `CargaInicial` crea estos datos semilla:

| Rol | Correo | Contraseña | Detalle |
|---|---|---|---|
| Administrador | `admin@valqui.com` | `Admin123*` | — |
| Compañía | `rentacol@valqui.com` | `Renta123*` | RentaCol, NIT `900123456-7`, Bogotá y Medellín, 24 h de cancelación gratuita, penalización 50000, depósito 300000 |
| Agente de sucursal | `agente.norte@valqui.com` | `Agente123*` | Sucursal Norte de RentaCol (id `suc-rentacol-norte`) |

Sucursales semilla de RentaCol: `suc-rentacol-norte` (Sucursal Norte, Bogotá) y `suc-rentacol-sur` (Sucursal Sur, Bogotá).

## Colección de Postman

`VAlqui.postman_collection.json` (formato v2.1): 9 carpetas y 49 requests, cada uno con `pm.test`. Las fechas se generan en el pre-request de la colección (recogida = ahora + 3 días, etc.), y los tokens, ids y códigos se guardan solos en variables de colección.

- **Postman:** *Import* → elige el archivo → *Run collection* (en orden).
- **Newman:**

```powershell
newman run VAlqui.postman_collection.json
```

Córrela sobre una base de datos vacía después de la carga inicial. El NIT, los correos y las placas llevan un sufijo por ejecución, así que también se puede volver a correr sin vaciar la base de datos.

## Arquitectura

```
com.valqui
├── presentacion   → FiltroSesion, UsuarioController, CompaniaController, VehiculoController,
│                    ReservaController, EntregaController, ManejadorErrores (+ dto/ con records)
├── negocio        → UsuarioService, CompaniaService, VehiculoService, ReservaService,
│                    EntregaService, PagoService (+ NegocioException)
├── datos          → UsuarioRepository, CompaniaRepository, VehiculoRepository, ReservaRepository
├── integracion    → IPasarelaPago, PasarelaPagoClient (simulador)
└── modelo         → Usuario {abstract}, Cliente, UsuarioCompania, AgenteSucursal, Administrador,
                     MetodoPago, CompaniaAliada, Sucursal, Cobertura, Vehiculo, Reserva,
                     PenalizacionDano, EstadoVehiculo, EstadoReserva
```

Controller → Service → Repository, con inyección por constructor. Colecciones de MongoDB: `usuarios` (las 4 subclases, con el campo `tipo`), `companias` (con sucursales y coberturas embebidas), `vehiculos` y `reservas`.

Sesión: `POST /api/auth/login` devuelve un JWT con el id, el correo y el tipo del usuario. Todas las rutas, salvo el login y el registro de clientes, exigen `Authorization: Bearer <token>` (401 si falta o es inválido, 403 si el rol no corresponde). Todos los errores responden `{ "mensaje": "...", "codigo": N }`.

## Endpoints

| Método y ruta | Rol | HU | Controller → Service | Respuestas |
|---|---|---|---|---|
| POST `/api/usuarios/clientes` | público | HU-01 | UsuarioController.registrarCliente → UsuarioService.registrarCliente | 201 · 400 · 409 |
| POST `/api/auth/login` | público | HU-18 | UsuarioController.iniciarSesion → UsuarioService.autenticar | 200 · 401 |
| POST `/api/clientes/{clienteId}/metodos-pago` | CLIENTE (el mismo) | R03 | UsuarioController.registrarMetodoPago → UsuarioService.agregarMetodoPago | 201 · 400 |
| POST `/api/companias` | ADMIN | HU-15 | CompaniaController.registrarCompania → CompaniaService.registrarCompania → UsuarioService.crearCuentaCompania | 201 · 409 |
| POST `/api/companias/{nit}/sucursales` | COMPANIA (su nit) | HU-09 | CompaniaController.registrarSucursal → CompaniaService.agregarSucursal | 201 · 400 |
| PUT `/api/companias/{nit}/politica` | COMPANIA (su nit) | HU-12 | CompaniaController.configurarPolitica → CompaniaService.configurarPolitica | 200 |
| POST `/api/companias/{nit}/coberturas` | COMPANIA (su nit) | HU-13 | CompaniaController.agregarCobertura → CompaniaService.agregarCobertura | 201 |
| POST `/api/vehiculos` | COMPANIA | HU-10 | VehiculoController.registrarVehiculo → VehiculoService.registrarVehiculo | 201 · 400 · 409 |
| GET `/api/vehiculos/disponibles?ciudad&desde&hasta&categoria&nit` | CLIENTE | HU-02 | VehiculoController.buscarDisponibles → VehiculoService.buscarDisponibles | 200 · 400 |
| GET `/api/vehiculos/{placa}` | CLIENTE | HU-03, R15 | VehiculoController.consultarDetalle → VehiculoService.consultarDetalle | 200 · 404 |
| PUT `/api/vehiculos/{placa}/sucursal` | COMPANIA (dueña) | HU-11 | VehiculoController.reasignar → VehiculoService.reasignar | 200 · 409 |
| PUT `/api/vehiculos/{placa}/estado` | COMPANIA (dueña) | HU-14 | VehiculoController.cambiarEstado → VehiculoService.cambiarEstado | 200 · 400 · 409 |
| POST `/api/reservas` | CLIENTE | HU-04 | ReservaController.crearReserva → ReservaService.crearReserva | 201 · 400 · 403 · 409 |
| POST `/api/reservas/{codigo}/coberturas` | CLIENTE dueño | HU-05 | ReservaController.agregarCobertura → ReservaService.agregarCobertura | 200 · 400 · 409 |
| POST `/api/reservas/{codigo}/confirmar` | CLIENTE dueño | HU-06 | ReservaController.confirmar → ReservaService.confirmar → PagoService.cobrar → IPasarelaPago | 200 · 400 · 403 · 409 |
| POST `/api/reservas/{codigo}/cancelar` | CLIENTE dueño | HU-07, R12 | ReservaController.cancelar → ReservaService.cancelar | 200 · 409 |
| GET `/api/reservas/cliente/{clienteId}` | CLIENTE (el mismo) | HU-08, R20 | ReservaController.consultarHistorial → ReservaService.consultarHistorial | 200 |
| POST `/api/entregas/{codigo}/entrega` | AGENTE | HU-16 | EntregaController.entregarVehiculo → EntregaService.entregar | 200 · 400 · 403 · 409 |
| POST `/api/entregas/{codigo}/devolucion` | AGENTE | HU-17 | EntregaController.recibirVehiculo → EntregaService.recibir | 200 · 403 · 409 |

### Ejemplos de cuerpo

- Registrar método de pago: `{ "marca": "VISA", "tipo": "CREDITO", "ultimosDigitos": "4242", "token": "tok_..." }`. La tarjeta llega tokenizada, así que nunca se recibe ni se guarda el número completo.
- Pasarela simulada: aprueba si el token no está vacío y el monto es mayor que 0; rechaza el token `tok_rechazado`.
- Crear reserva: `{ "placa": "...", "fechaHoraRecogida": "2026-12-01T10:00:00", "fechaHoraDevolucion": "2026-12-03T10:00:00", "sucursalRecogidaId": "...", "sucursalDevolucionId": "..." }`.

## Ajustes sobre las firmas del diagrama de desarrollo

Para cumplir las HU sin agregar funcionalidad, algunas firmas llevan un parámetro más:

- `UsuarioService.crearCuentaCompania(nit, correo, representante)` devuelve la contraseña temporal, porque la HU-15 también pide nombre, cargo y teléfono del representante.
- `CompaniaService.registrarCompania(compania, correoAcceso, representante)` devuelve la contraseña temporal para mostrarla una sola vez.
- `Reserva.cancelar(fecha, horasCancelacionGratuita, penalizacionCancelacion)`: la política es de la compañía y la reserva no la copia.
- `ReservaService.cancelar(codigo)` devuelve la penalización (`double`), que es lo que responde el endpoint.
- `ReservaService.iniciarAlquiler` y `finalizarAlquiler` reciben al `AgenteSucursal` de la sesión, para validar su sucursal mediante `entregarVehiculo` y `recibirVehiculo`.
- `VehiculoService.registrarEntrega(placa)` pone el vehículo en RESERVADO (en uso) al entregarlo, porque `cambiarEstado` no permite asignar RESERVADO a mano.
- `VehiculoService.consultarDetalle(placa)` arma el detalle de HU-03/R15 (el controller solo depende de VehiculoService).
