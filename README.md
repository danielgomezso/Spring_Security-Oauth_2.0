# Clínica Demo · Sistema de Citas Médicas

Plataforma integral de gestión de citas médicas diseñada con una arquitectura desacoplada y segura:
* **Backend:** Spring Boot 3.5 con **Spring Authorization Server (OAuth 2.0 / OpenID Connect)** y **Resource Server**.
* **Frontend:** Angular Standalone Components con enrutamiento protegido por roles e interceptores Bearer.

---

## 🏛️ Arquitectura de Seguridad (OAuth 2.0 + Resource Server)

El backend combina en un solo servicio las capacidades de un **Servidor de Autorización OAuth 2.0** y un **Servidor de Recursos**:

```
[ Frontend Angular ] 
       │
       ├─► 1. Autenticación / Token ──► [ Spring Authorization Server ]
       │                                     • Claves asimétricas RSA (2048-bit)
       │                                     • /.well-known/openid-configuration
       │                                     • /oauth2/jwks
       │                                     • /oauth2/token (PKCE & Client Credentials)
       │
       └─► 2. API de Citas (Bearer)  ──► [ Spring Resource Server ]
                                             • Validación asimétrica RS256
                                             • Control de acceso basado en roles (RBAC)
```

### Características de Seguridad:
1. **Firma Asimétrica (RS256):** Los tokens JWT se firman mediante un par de claves RSA de 2048 bits generadas en el servidor. La clave pública se expone automáticamente en el endpoint estándar `/oauth2/jwks`.
2. **Validación Estricta:** El Resource Server valida la firma criptográfica, la vigencia temporal y el emisor (`iss`) del token contra el Authorization Server.
3. **Clientes OAuth 2.0 Registrados:**
   * `citas-frontend`: Cliente público para la SPA en Angular, configurado con soporte obligatorio de **PKCE** (*Proof Key for Code Exchange*).
   * `citas-service`: Cliente confidencial para integración entre servicios mediante *Client Credentials Grant*.

---

## 👥 Cuentas y Credenciales de Demostración

El sistema inicializa automáticamente las siguientes cuentas con contraseñas cifradas en **BCrypt**:

| Rol | Usuario | Contraseña | Permisos |
|---|---|---|---|
| **Paciente (`ROLE_USER`)** | `paciente1` | `password123` | Consultar agenda personal y reservar nuevas citas |
| **Administrador (`ROLE_ADMIN`)** | `admin1` | `admin123` | Visualizar todas las citas del sistema y cancelar reservas |
| **Cliente OAuth 2.0** | `citas-service` | `secret123` | Generar tokens máquina a máquina (*Client Credentials*) |

---

## 🚀 Puesta en Marcha

### Requisitos Previos
* **Java 17 o superior** (OpenJDK 21 recomendado)
* **Node.js 18+** y **npm**

### 1. Iniciar el Backend
```bash
cd backend
./mvnw spring-boot:run
```
* Por defecto utiliza la base de datos **H2 en memoria**.
* Consola H2 disponible en: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:citasdb`, Usuario: `sa`, Contraseña vacía).
* Para ejecutar con **SQL Server**:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=sqlserver
  ```
  *(Asegúrate de ejecutar previamente los scripts en `backend/src/main/resources/db/sqlserver/`).*

### 2. Iniciar el Frontend
```bash
cd frontend
npm install
npm start
```
* La aplicación estará disponible en [http://localhost:4200](http://localhost:4200).

---

## 🧪 Pruebas Automatizadas

El backend incluye una suite completa de pruebas de integración que valida el ciclo completo de seguridad y negocio:

```bash
cd backend
./mvnw test
```

### Casos cubiertos:
* Descubrimiento de metadatos OIDC (`/.well-known/openid-configuration`).
* Consulta de llaves públicas JWKS (`/oauth2/jwks`).
* Emisión de tokens OAuth 2.0 mediante *Client Credentials*.
* Aislamiento estricto de pacientes (un paciente solo ve sus propias citas).
* Rechazo de tokens manipulados (*tampered*) o con emisor incorrecto.
* Restricciones de autorización basadas en roles (`ROLE_USER` vs `ROLE_ADMIN`).
* Validación de slots duplicados y prevención de conflictos de horario médico.
* Negociación CORS preflight para la SPA.

---

## 📡 Catálogo de Endpoints

### 🔓 Públicos y OAuth 2.0
* `GET /.well-known/openid-configuration`: Metadatos de descubrimiento del servidor OAuth 2.0.
* `GET /oauth2/jwks`: Conjunto de llaves públicas RSA para validación de tokens.
* `POST /oauth2/token`: Endpoint de emisión de tokens OAuth 2.0.
* `GET /publico/especialidades`: Listado público de especialidades médicas disponibles.
* `POST /auth/login`: Autenticación directa por credenciales para la interfaz web.

### 🔒 Panel del Paciente (`ROLE_USER`)
* `GET /api/citas/mis-citas`: Lista ordenada de citas del paciente autenticado.
* `POST /api/citas`: Reserva de nueva consulta médica (asocia automáticamente al usuario del token).

### 🔒 Administración (`ROLE_ADMIN`)
* `GET /api/admin/citas`: Visualización global de todas las citas agendadas.
* `DELETE /api/admin/citas/{id}`: Eliminación/cancelación de una cita.
