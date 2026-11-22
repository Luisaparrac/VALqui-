# PROMPT — Backend VAlqui (Java 17 + Spring Boot + MongoDB + Postman)

Adjunto a este mensaje los 5 diagramas de arquitectura del proyecto VAlqui (contexto, funcional, lógica, desarrollo y despliegue). Necesito que construyas el **backend completo** que implemente **exactamente** lo que dicen estos diagramas y las historias de usuario de abajo: **ni más ni menos**. No agregues funcionalidades, entidades, endpoints, roles ni campos que no estén aquí. Si algo no está definido, usa la decisión indicada en este prompt; si aun así falta algo, pregúntame antes de inventarlo.

---

## 1. Tecnología (obligatoria)

- Java 17, Spring Boot 3.x, Maven.
- Dependencias permitidas: `spring-boot-starter-web`, `spring-boot-starter-data-mongodb`, `spring-boot-starter-validation`, `spring-security-crypto` (solo para BCrypt; **no** uses Spring Security completo) y `jjwt` para el token de sesión. Nada más.
- MongoDB: la URI se lee de la variable de entorno `MONGODB_URI` (en producción es MongoDB Atlas sobre TLS); por defecto `mongodb://localhost:27017/valqui`.
- Puerto local 8080.
- Un solo artefacto: `valqui-api.jar` (las 3 capas viven en el mismo jar, como en la vista de despliegue).

## 2. Arquitectura (vista de desarrollo)

API REST en 3 capas. Cada capa solo llama a la de abajo: **Controller → Service → Repository**.

```
com.valqui
├── presentacion   → FiltroSesion + 5 RestControllers + ManejadorErrores
├── negocio        → 6 Services
├── datos          → 4 repositorios (interfaces que extienden MongoRepository)
├── integracion    → IPasarelaPago + PasarelaPagoClient
└── modelo         → clases de dominio de la vista lógica + 2 enums
```

- Los nombres de clases, atributos y métodos deben ser **exactamente los de los diagramas, en español**.
- Los mensajes que ve el usuario van en español.
- Las dependencias entre clases deben ser las mismas del diagrama de desarrollo:
  - `ReservaService` usa: ReservaRepository, VehiculoService, UsuarioService, CompaniaService y PagoService.
  - `VehiculoService` usa: VehiculoRepository, **ReservaRepository** (no ReservaService, para evitar el ciclo) y CompaniaService.
  - `EntregaService` usa ReservaService y VehiculoService.
  - `CompaniaService` usa CompaniaRepository y UsuarioService.
  - `PagoService` usa IPasarelaPago y UsuarioService.
  - `UsuarioService` usa UsuarioRepository.
- Inyección por constructor.

**Únicas clases técnicas extra permitidas** (no son de negocio):

- `ManejadorErrores` (`@RestControllerAdvice`).
- Una excepción de negocio que lleve el código HTTP.
- DTOs de request/response (records).
- `CargaInicial`, que crea los datos semilla (sección 7).

## 3. Modelo de dominio (vista lógica) y colecciones MongoDB

### Clases del dominio

**Usuario {abstract}**
- Atributos: nombreCompleto, correo (único), contrasena (guardada con BCrypt).
- Método: iniciarSesion(correo, contrasena): boolean.

**Subclases de Usuario.** Las 4 se guardan en **una sola colección `usuarios`**, con un campo `tipo` que puede ser CLIENTE, COMPANIA, AGENTE o ADMIN.

| Subclase | Atributos | Métodos |
|---|---|---|
| Cliente | fechaNacimiento: LocalDate, numeroContacto, paisResidencia, lista de **MetodoPago embebidos** (composición) | esMayorDeEdad() (18 años o más), agregarMetodoPago(metodoPago) |
| UsuarioCompania | cargo, telefono, nit de la compañía que representa | — |
| AgenteSucursal | codigoEmpleado, sucursalId y nit de la compañía (relación «trabaja en») | entregarVehiculo(reserva, documentosValidos), recibirVehiculo(reserva, valorDano, motivoDano) |
| Administrador | codigoAdministrador | registrarCompania(compania) |

**MetodoPago**
- Atributos: marca, tipo (CREDITO o DEBITO), ultimosDigitos (4), token.
- Método: esMarcaValida(), que acepta solo VISA, MASTERCARD o AMERICAN_EXPRESS.
- **Nunca** guardes el número completo de la tarjeta.

**CompaniaAliada** — colección `companias`
- Atributos: nit (único), nombre, ciudadesOperacion (1 o más), valorDeposito, horasCancelacionGratuita, penalizacionCancelacion (monto fijo).
- Lleva embebidas sus **Sucursal** y **Cobertura** (composición). A cada sucursal y cada cobertura embebida asígnale un `id` propio.
- Métodos:
  - operaEn(ciudad).
  - agregarSucursal(sucursal): rechaza la sucursal si su ciudad no está en ciudadesOperacion.
  - agregarCobertura(cobertura).

**Sucursal** (embebida en la compañía)
- Atributos: nombre, ciudad, direccion, coordenadas [opcional], horarioAtencion.

**Cobertura** (embebida en la compañía)
- Atributos: nombre, costo.

**Vehiculo** — colección `vehiculos`
- Atributos: placa (única en toda la plataforma), marca, modelo, categoria, anio, transmision, capacidadPasajeros, tarifaDiaria, kilometrajeIncluido (Integer; `null` = ilimitado), franquiciaDanos, estado: EstadoVehiculo.
- Referencias: `companiaNit` (relación «es dueña de») y la sucursal asignada actual: `sucursalId` + `sucursalCiudad`, que se copia para poder filtrar por ciudad.
- Métodos: estaDisponible(desde, hasta), tieneReservaActiva(), cambiarEstado(estado), reasignar(sucursal).

**Reserva** — colección `reservas`
- Atributos: codigo (único, legible, por ejemplo `RES-XXXXXX`), fechaHoraRecogida, fechaHoraDevolucion, estado: EstadoReserva.
- Copias tomadas al crearla:
  - Del vehículo: tarifaDiaria, kilometrajeIncluido y franquiciaDanos.
  - De la compañía: valorDeposito.
- Referencias:
  - clienteId («realiza»).
  - vehiculoPlaca.
  - sucursalRecogidaId y sucursalDevolucionId (pueden ser distintas).
  - coberturaIds («incluye», 0..*).
  - metodoPago usado (ultimosDigitos, relación «se paga con» 0..1).
- Embebida: **PenalizacionDano** [0..1], con valor y motivo (composición).
- Métodos: calcularTotal(), agregarCobertura(), confirmar(metodoPago), cancelar(fecha): double, iniciarAlquiler(), finalizarAlquiler(), registrarPenalizacion(valor, motivo), estaActiva(), seCruzaCon(desde, hasta).

**Enums**
- `EstadoVehiculo`: DISPONIBLE, RESERVADO, EN_MANTENIMIENTO, FUERA_DE_SERVICIO.
- `EstadoReserva`: PENDIENTE, CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA.

### Interfaz de pago

- `IPasarelaPago.procesarCobro(token, monto): boolean`. La pasarela es externa y no se construye.
- `PasarelaPagoClient` implementa esa interfaz con los atributos `urlApi` y `apiKey`, leídos de `application.properties`. Funciona como **simulador**:
  - Aprueba si el token no está vacío y el monto es mayor que 0.
  - Rechaza si el token es exactamente `tok_rechazado`.

### Reglas de negocio de las clases

- **Reserva activa** = PENDIENTE, CONFIRMADA o EN_CURSO.
- **Cruce de fechas**: `recogidaA < devolucionB && devolucionA > recogidaB`.
- **calcularTotal()** = días × tarifaDiaria + suma del costo de las coberturas incluidas.
  - Días = horas entre recogida y devolución ÷ 24, redondeado hacia arriba, mínimo 1.
  - El depósito es reembolsable: se informa pero **no** se suma al cobro.

## 4. Endpoints (vista de desarrollo + HU)

Los códigos HTTP son los vistos en clase: 200, 201, 400, 404, 409 y 500, más 401 y 403 para la sesión.

Todas las respuestas de error tienen este formato:

```json
{ "mensaje": "...", "codigo": 409 }
```

Usa los mensajes de las HU **literalmente**.

### FiltroSesion (`«Filter»`)

- Revisa `Authorization: Bearer <token>` en todas las rutas, excepto `POST /api/auth/login` y `POST /api/usuarios/clientes`.
- Llama a `usuarioService.validarSesion(token)`.
- Si falta el token o es inválido → 401.
- Si el rol no corresponde a la ruta → 403.
- El token es un JWT con el id, el correo y el tipo de usuario.

### Endpoints de UsuarioController

| Método y ruta | Rol | Qué hace | Respuestas |
|---|---|---|---|
| POST /api/usuarios/clientes | público | registrarCliente (HU-01) | 201 · 400 datos inválidos · 409 "Ya existe un usuario registrado con ese correo" |
| POST /api/auth/login | público | iniciarSesion (HU-18): devuelve token, tipo e id | 200 · 401 "Correo o contraseña incorrectos" |
| POST /api/clientes/{clienteId}/metodos-pago | CLIENTE (solo el mismo cliente) | registrarMetodoPago (R03) | 201 · 400 "Marca de tarjeta no aceptada" |

### Endpoints de CompaniaController

| Método y ruta | Rol | Qué hace | Respuestas |
|---|---|---|---|
| POST /api/companias | ADMIN | registrarCompania (HU-15), detalle abajo | 201 · 409 "Ya existe una compañía registrada con ese NIT" |
| POST /api/companias/{nit}/sucursales | COMPANIA (su propio nit) | registrarSucursal (HU-09) | 201 · 400 "La compañía no opera en esa ciudad" |
| PUT /api/companias/{nit}/politica | COMPANIA | configurarPolitica (HU-12): horasCancelacionGratuita, penalizacionCancelacion, valorDeposito | 200 |
| POST /api/companias/{nit}/coberturas | COMPANIA | agregarCobertura (HU-13) | 201 |

Detalle de registrarCompania (HU-15):
- Recibe nit, nombre, ciudadesOperacion y correoAcceso, más nombreCompleto, cargo y telefono del representante.
- Llama a `usuarioService.crearCuentaCompania(nit, correo)`, que crea el UsuarioCompania con una **contraseña temporal generada**.
- La contraseña temporal se devuelve una sola vez en la respuesta.

### Endpoints de VehiculoController

| Método y ruta | Rol | Qué hace | Respuestas |
|---|---|---|---|
| POST /api/vehiculos | COMPANIA | registrarVehiculo (HU-10), queda en DISPONIBLE | 201 · 409 "Ya existe un vehículo registrado con esa placa" · 400 si la sucursal no es de su compañía |
| GET /api/vehiculos/disponibles?ciudad&desde&hasta&categoria&nit | CLIENTE | buscarDisponibles (HU-02), detalle abajo | 200 · 400 si faltan ciudad o fechas |
| GET /api/vehiculos/{placa} | CLIENTE | consultarDetalle (HU-03, R15), detalle abajo | 200 · 404 |
| PUT /api/vehiculos/{placa}/sucursal | COMPANIA | reasignar (HU-11), solo a una sucursal de la misma compañía | 200 · 409 "No se puede reasignar un vehículo con una reserva activa" |
| PUT /api/vehiculos/{placa}/estado | COMPANIA | cambiarEstado (HU-14), solo entre DISPONIBLE, EN_MANTENIMIENTO y FUERA_DE_SERVICIO | 200 · 400 si se intenta poner RESERVADO · 409 "No se puede cambiar el estado de un vehículo con una reserva activa" |

Detalle de buscarDisponibles (HU-02):
- Ciudad y fechas son obligatorias; categoría y nit son opcionales.
- Solo aparecen vehículos en estado DISPONIBLE o RESERVADO, **sin una reserva activa que se cruce** con las fechas.
- Si no hay resultados, responde 200 con `vehiculos: []` y el mensaje "No se encontraron vehículos disponibles con los filtros seleccionados".

Detalle de consultarDetalle (HU-03, R15): muestra el vehículo junto con el depósito, la franquicia, el kilometraje incluido, la política de cancelación de la compañía y las coberturas que ofrece esa compañía.

### Endpoints de ReservaController

| Método y ruta | Rol | Qué hace | Respuestas |
|---|---|---|---|
| POST /api/reservas | CLIENTE | crearReserva (HU-04), detalle abajo | 201 · 400 fechas inválidas · 403 "Debes ser mayor de edad para reservar" · 409 "El vehículo no está disponible en esas fechas" |
| POST /api/reservas/{codigo}/coberturas | CLIENTE dueño | agregarCobertura (HU-05), detalle abajo | 200 · 400 · 409 |
| POST /api/reservas/{codigo}/confirmar | CLIENTE dueño | confirmar (HU-06), detalle abajo | 200 · 400 "Debes registrar un método de pago" · 403 menor de edad · 409 "Pago rechazado" |
| POST /api/reservas/{codigo}/cancelar | CLIENTE dueño | cancelar (HU-07, R12), detalle abajo | 200 · 409 |
| GET /api/reservas/cliente/{clienteId} | CLIENTE (el mismo) | consultarHistorial (HU-08, R20) | 200 |

Detalle de crearReserva (HU-04):
- Recibe placa, fechaHoraRecogida, fechaHoraDevolucion, sucursalRecogidaId y sucursalDevolucionId.
- Las sucursales deben ser de la compañía dueña del vehículo.
- La recogida debe ser futura y la devolución posterior a la recogida.
- La reserva se crea en PENDIENTE, con los valores copiados del vehículo y la compañía.

Detalle de agregarCobertura (HU-05):
- Solo en reservas PENDIENTE.
- La cobertura debe ser de la compañía dueña del vehículo.

Detalle de confirmar (HU-06):
- Recibe ultimosDigitos de un método de pago ya registrado.
- Vuelve a revisar la mayoría de edad (R16).
- Cobra calcularTotal() con PagoService → IPasarelaPago.
- Pasa la reserva a CONFIRMADA.

Detalle de cancelar (HU-07, R12):
- Solo en PENDIENTE (siempre sin costo) o en CONFIRMADA.
- Si quedan al menos horasCancelacionGratuita antes de la recogida, la penalización es 0; si no, se aplica penalizacionCancelacion.
- Pasa la reserva a CANCELADA y devuelve `{ penalizacion }`.
- En cualquier otro estado → 409 "La reserva no se puede cancelar en su estado actual".

### Endpoints de EntregaController

| Método y ruta | Rol | Qué hace | Respuestas |
|---|---|---|---|
| POST /api/entregas/{codigo}/entrega | AGENTE | entregarVehiculo (HU-16), detalle abajo | 200 · 400 · 403 · 409 si la reserva no está CONFIRMADA |
| POST /api/entregas/{codigo}/devolucion | AGENTE | recibirVehiculo (HU-17), detalle abajo | 200 · 409 "La reserva ya tiene una penalización por daño registrada" · 409 si no está EN_CURSO |

Detalle de entregarVehiculo (HU-16):
- Recibe `{ documentosValidos }`.
- La reserva debe estar CONFIRMADA y su sucursal de recogida debe ser la del agente; si no → 403.
- Si los documentos son válidos: la reserva pasa a EN_CURSO y el vehículo a RESERVADO.
- Si no son válidos: 400 "Documentos no válidos, no se puede iniciar el alquiler" y la reserva sigue CONFIRMADA.

Detalle de recibirVehiculo (HU-17):
- Recibe `{ valorDano?, motivoDano? }`.
- La reserva debe estar EN_CURSO y su sucursal de devolución debe ser la del agente.
- Si se envía daño, se registra la PenalizacionDano (máximo una por reserva).
- La reserva pasa a FINALIZADA.
- El vehículo vuelve a DISPONIBLE y queda asignado a la **sucursal de devolución**, mediante `vehiculoService.registrarDevolucion`.

## 5. Decisiones ya tomadas (no las cambies)

- El vehículo define la tarifa, el kilometraje y la franquicia. La compañía define el depósito y la política. La reserva **copia** esos valores, así que un cambio posterior no afecta las reservas existentes.
- "Reservado" significa **en uso**: lo pone el sistema en la entrega y lo quita en la devolución. La compañía nunca lo asigna a mano.
- Un vehículo RESERVADO sí aparece en la búsqueda para fechas que no se cruzan con su reserva activa.
- Solo se guarda la sucursal actual del vehículo; no hay historial de asignaciones.
- Mayoría de edad = 18 años o más.
- Los documentos de identidad y la licencia **no** se suben (R02). Solo se registra si fueron válidos.
- Fuera de alcance: editar o eliminar entidades, recuperar contraseña, crear agentes por API y cobrar el depósito.

## 6. Colección de Postman (obligatoria)

Entrega `VAlqui.postman_collection.json` (formato v2.1):

- Debe correr en orden con el Runner o con Newman sobre una base de datos **vacía**, después de la carga inicial, y pasar el **100 %** de los tests.
- Usa variables de colección: `baseUrl`, `tokenAdmin`, `tokenCompania`, `tokenCliente`, `tokenAgente`, `clienteId`, `codigoReserva`, etc. Los scripts de test las guardan automáticamente.
- Las fechas se generan dinámicamente en scripts pre-request, por ejemplo recogida = ahora + 3 días.
- Cada request tiene `pm.test` que verifica el código HTTP y los campos clave de la respuesta.

### Carpetas y casos (un request por caso, en este orden)

**1. Sesión (HU-18)**
1. Login del administrador → 200.
2. Login con contraseña incorrecta → 401.

**2. Compañías (HU-15, HU-09, HU-12, HU-13)**
1. Registrar "AutoAndes" → 201. Guarda la contraseña temporal.
2. Registrar con un NIT duplicado → 409.
3. Login de la compañía → 200.
4. Registrar una sucursal → 201.
5. Registrar una sucursal en una ciudad donde la compañía no opera → 400.
6. Configurar la política → 200.
7. Agregar una cobertura → 201.

**3. Vehículos (HU-10, HU-14, HU-11)**
1. Registrar un vehículo → 201.
2. Registrar con una placa duplicada → 409.
3. Cambiar el estado a EN_MANTENIMIENTO → 200.
4. Volver a DISPONIBLE → 200.
5. Intentar poner RESERVADO a mano → 400.

**4. Cliente (HU-01, R03)**
1. Registrar un cliente mayor de edad → 201.
2. Registrar con un correo duplicado → 409.
3. Login → 200.
4. Registrar una tarjeta VISA → 201.
5. Registrar una marca inválida → 400.
6. Registrar un cliente menor de edad y hacer su login → 201 y 200.

**5. Búsqueda y detalle (HU-02, HU-03)**
1. Búsqueda con resultados → 200, la lista contiene la placa.
2. Búsqueda sin ciudad → 400.
3. Búsqueda sin resultados → 200 con lista vacía y el mensaje.
4. Detalle del vehículo → 200 con depósito, franquicia, kilometraje y política.

**6. Reservas (HU-04 a HU-08)**
1. Crear una reserva con el cliente menor de edad → 403.
2. Crear una reserva → 201, en PENDIENTE.
3. Crear otra reserva que se cruza con la anterior → 409.
4. Agregar una cobertura → 200.
5. Confirmar sin un método de pago válido (ultimosDigitos inexistentes) → 400.
6. Confirmar → 200, en CONFIRMADA.
7. Cambiar el estado del vehículo con reserva activa → 409.
8. Reasignar el vehículo con reserva activa → 409.
9. Crear una segunda reserva en otras fechas y cancelarla → 200 con penalización 0.
10. Consultar el historial → 200.

**7. Entrega y devolución (HU-16, HU-17)**
1. Login del agente → 200.
2. Entrega con documentos no válidos → 400 y la reserva sigue CONFIRMADA.
3. Entrega válida → 200: la reserva queda EN_CURSO y el vehículo RESERVADO.
4. Devolución con daño → 200: la reserva queda FINALIZADA y el vehículo DISPONIBLE.
5. Segunda penalización sobre la misma reserva → 409.

**8. Pago rechazado**
1. Registrar una tarjeta con token `tok_rechazado`.
2. Crear una reserva nueva.
3. Confirmarla con esa tarjeta → 409 "Pago rechazado".

**9. Seguridad**
1. Request sin token → 401.
2. Cliente intentando registrar una compañía → 403.

**Importante:** el agente semilla trabaja en una sucursal de la compañía semilla "RentaCol". Por eso las reservas que se entregan en la carpeta 7 deben hacerse sobre un vehículo de **RentaCol**: regístralo con el token de RentaCol en la carpeta 3. El vehículo de "AutoAndes" se usa para el resto de casos.

## 7. Carga inicial (`CargaInicial`, solo si la base está vacía)

- Administrador: `admin@valqui.com` / `Admin123*`.
- Compañía "RentaCol":
  - NIT `900123456-7`.
  - Ciudades: Bogotá y Medellín.
  - Política: 24 h de cancelación gratuita, penalización 50000, depósito 300000.
  - Sucursales: "Sucursal Norte" (Bogotá) y "Sucursal Sur" (Bogotá).
  - Usuario de la compañía: `rentacol@valqui.com` / `Renta123*`.
- Agente de sucursal: `agente.norte@valqui.com` / `Agente123*`, en Sucursal Norte (las HU dicen que los agentes se crean fuera del backlog).

## 8. Verificación antes de entregar

1. Levanta MongoDB y la aplicación, y **ejecuta la colección con Newman**. Muéstrame el resumen: todos los tests en verde.
   - Si no puedes instalar MongoDB en tu entorno, usa MongoDB embebido (flapdoodle) **solo para la verificación**, sin dejarlo en el `pom.xml` final.
   - Dime claramente qué ejecutaste y qué no.
2. Revisa contra este prompt que no sobre nada: ningún endpoint, clase de negocio, campo ni rol extra.

## 9. Entregables

- Un **ZIP** con el proyecto Maven completo: `pom.xml`, `src/` y `application.properties` con `MONGODB_URI` y los datos simulados de la pasarela.
- `VAlqui.postman_collection.json`.
- `README.md` con: requisitos, cómo correrlo (`mvn spring-boot:run` y `java -jar valqui-api.jar`), cómo configurar Atlas, cómo importar y correr la colección, y una tabla de endpoints que muestre de qué HU y de qué clase del diagrama sale cada uno.
- Si haces commits de git: el autor es **Luisa Fernanda Parra Cabrera <luisa.parrac@campusucc.edu.co>**, **sin** líneas `Co-authored-by` ni ninguna otra atribución.
