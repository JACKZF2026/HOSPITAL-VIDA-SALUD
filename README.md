# Hospital Vida Salud — Plataforma de Microservicios

Solución académica basada en microservicios para los procesos administrativos del
Hospital Vida Salud. **Todos los datos utilizados son ficticios**; el sistema no
debe usarse para diagnosticar ni tomar decisiones clínicas reales.

## Arquitectura

```
Cliente / Personal autorizado
        |
        v
 API Gateway (Spring Cloud Gateway)  :8080
        |
        +--> Eureka Discovery Server  :8761
        |
+-------+---------+-----------+------------+---------+-------------+------------+
| Pacientes       | Citas     | Atenciones | Laborat.| Pagos       | Notif./Aud.|
| :8081           | :8082     | :8083      | :8084   | :8085       | :8086/:8087|
+-------+---------+-----------+------------+---------+-------------+------------+
        |  H2 (SQL) + Spring Data R2DBC por servicio (base propia por microservicio)
        |
        +---> RabbitMQ (exchange topic: hospital.eventos)
                      |--> notificaciones-service (notificaciones.eventos)
                      |--> auditoria-service      (auditoria.eventos, binding #)
```

| Módulo | Puerto | Responsabilidad |
|---|---|---|
| `discovery-server` | 8761 | Servidor Eureka: registro y descubrimiento |
| `api-gateway` | 8080 | Punto de entrada único; enruta a `lb://<servicio>` |
| `pacientes-service` | 8081 | Registro/consulta/actualización de pacientes; documento único |
| `citas-service` | 8082 | Programación, confirmación, reprogramación y estados de citas |
| `atenciones-service` | 8083 | Atención administrativa vinculada a una cita CONFIRMADA |
| `laboratorio-service` | 8084 | Solicitudes de exámenes ficticios y resultados de prueba |
| `pagos-service` | 8085 | Pagos simulados: monto > 0, operación única |
| `notificaciones-service` | 8086 | Consume eventos RabbitMQ y genera avisos simulados |
| `auditoria-service` | 8087 | Consume todos los eventos y guarda trazabilidad |

## Comunicación

- **Síncrona (WebClient + Eureka/LoadBalancer):**
  - `citas-service → pacientes-service` (valida que el paciente exista y esté ACTIVO)
  - `atenciones-service → citas-service` (valida la cita CONFIRMADA y la marca ATENDIDA)
  - `laboratorio-service → atenciones-service` (valida la atención)
  - `pagos-service → pacientes-service` (valida el paciente)
- **Asíncrona (RabbitMQ, exchange `hospital.eventos`, tipo topic):**
  - `paciente.registrado`, `cita.creada`, `cita.confirmada`, `cita.cancelada`,
    `cita.reprogramada`, `cita.atendida`, `cita.ausente`, `atencion.registrada`,
    `atencion.actualizada`, `laboratorio.actualizado`, `pago.registrado`, `pago.anulado`

## Requisitos

- JDK 21+ (probado también con JDK 25)
- Maven (o el Maven Wrapper incluido `mvnw`/`mvnw.cmd`)
- Opcional: Docker para RabbitMQ y PostgreSQL (`docker-compose.yml`)

## Compilar y probar

```bash
# Linux / macOS
./mvnw clean verify

# Windows
.\mvnw.cmd clean verify
```

## Ejecutar la plataforma

1. **(Opcional) Infraestructura:** `docker compose up -d` levanta RabbitMQ
   (:5672, consola :15672) y PostgreSQL. Sin RabbitMQ los servicios arrancan
   igualmente, pero los eventos no se entregan (degradación elegante, ver logs).
2. **Eureka:** `java -jar discovery-server/target/discovery-server-1.0.0.jar`
3. **Gateway:** `java -jar api-gateway/target/api-gateway-1.0.0.jar`
4. **Servicios:** `java -jar <modulo>/target/<modulo>-1.0.0.jar`

El panel de Eureka está en `http://localhost:8761`.

## Flujo de prueba (a través del Gateway)

```bash
# 1. Registrar paciente
curl -X POST http://localhost:8080/api/pacientes -H "Content-Type: application/json" -d '{
  "tipoDocumento":"DNI","numeroDocumento":"10203040","nombres":"Ana","apellidos":"Torres Ficticia",
  "fechaNacimiento":"1992-07-15","telefono":"987654321","correo":"ana.ficticia@example.com"}'

# 2. Duplicado -> 409
curl -X POST http://localhost:8080/api/pacientes -H "Content-Type: application/json" -d '{...mismo documento...}'

# 3. Programar cita (pacienteId=1)
curl -X POST http://localhost:8080/api/citas -H "Content-Type: application/json" -d '{
  "pacienteId":1,"especialidad":"Medicina General","fechaHora":"2026-10-20T10:00:00",
  "profesionalCodigo":"PRO-01","motivo":"Control anual"}'

# 4. Confirmar cita (publica cita.confirmada)
curl -X PATCH http://localhost:8080/api/citas/1/estado -H "Content-Type: application/json" -d '{"estado":"CONFIRMADA"}'

# 5. Registrar atención (marca la cita ATENDIDA de forma síncrona)
curl -X POST http://localhost:8080/api/atenciones -H "Content-Type: application/json" -d '{"citaId":1,"area":"Consultorio"}'

# 6. Solicitud de laboratorio ficticia
curl -X POST http://localhost:8080/api/laboratorio -H "Content-Type: application/json" -d '{"atencionId":1,"tipoExamen":"Hemograma de prueba"}'

# 7. Pago simulado
curl -X POST http://localhost:8080/api/pagos -H "Content-Type: application/json" -d '{
  "pacienteId":1,"concepto":"Consulta externa","monto":120.50,"medioPago":"YAPE","numeroOperacion":"OP-TEST-0001"}'

# 8. Notificaciones y auditoría generadas por eventos
curl http://localhost:8080/api/notificaciones
curl http://localhost:8080/api/auditoria
```

## Configuración por variables de entorno

| Variable | Uso | Defecto |
|---|---|---|
| `EUREKA_URI` | URL del servidor Eureka | `http://localhost:8761/eureka/` |
| `RABBITMQ_HOST/PORT/USER/PASSWORD` | Broker AMQP | `localhost/5672/guest/guest` |
| `RABBITMQ_LISTENERS` | Arranque de consumidores | `true` |
| `R2DBC_URL` | URL R2DBC (p. ej. `r2dbc:postgresql://localhost:5432/vidasalud`) | H2 en memoria |

No se versionan credenciales: solo valores por defecto de desarrollo o
variables de entorno.

## Integración continua

`.github/workflows/ci.yml` ejecuta `./mvnw --batch-mode verify` en cada `push`
y `pull_request` a `main`: compila los 9 módulos y corre todas las pruebas
(unitarias, de controlador, integración H2/R2DBC y componentes de mensajería).
Si la compilación o una prueba falla, el job se marca en rojo y se suben los
reportes de Surefire como artefacto.
