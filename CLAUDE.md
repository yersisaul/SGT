# CLAUDE.md — SGT (Sistema de Gestión de Tickets)

# Preferencias de idioma
- Responde siempre en español a todas mis preguntas, explicaciones y comandos.

# 1. LOGICA DEL NEGOCIO
- Este es el flujo de negocio definitivo:

    1. Cliente registra una Solicitud de atención.
    2. Cliente puede consultar y hacer seguimiento de sus propias solicitudes.
    3. Despachador revisa y clasifica la solicitud.
    4. Despachador determina si corresponde a un servicio bajo contrato.
    5. Si está bajo contrato:
    Solicitud -> Orden de Trabajo.
    6. Si está fuera de contrato:
    Despachador crea un Requerimiento.
    7. Administrador (Gerente/Jefe) revisa el Requerimiento.
    8. Administrador puede aprobar o rechazar.
    9. Si se aprueba:
    Requerimiento -> Orden de Trabajo.
    10. Operaciones recibe la Orden de Trabajo.
    11. Operaciones ejecuta el soporte.
    12. Operaciones cierra la Orden de Trabajo.

- Flujo del negocio
                    ┌──────────────┐
                    │   CLIENTE    │
                    └──────┬───────┘
                           │
                           │ Registrar Solicitud
                           ▼
                    ┌──────────────┐
                    │  SOLICITUD   │
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │ DESPACHADOR  │
                    └──────┬───────┘
                           │
                  ¿Está bajo contrato?
                     /             \
                   SÍ               NO
                   │                 │
                   ▼                 ▼
             Generar OT        Crear RQ
                   │                 │
                   │                 ▼
                   │          ┌──────────────┐
                   │          │ ADMINISTRADOR│
                   │          └──────┬───────┘
                   │                 │
                   │             ¿Aprueba?
                   │              /     \
                   │            NO       SÍ
                   │            │         │
                   │           FIN        ▼
                   │                    Generar OT
                   │                       │
                   └───────────┬───────────┘
                               │
                               ▼
                       ┌────────────────┐
                       │  OPERACIONES   │
                       └───────┬────────┘
                               │
                               ▼
                       Ejecutar soporte
                               │
                               ▼
                         Cerrar OT

# 2. OBJETIVO DEL PROYECTO
SGT es un sistema de gestión de soporte técnico y requerimientos desarrollado con:
- Java
- Spring Boot
- Spring Data JPA / Hibernate
- Postgres
- Maven
- Lombok
- Jackson
- DTOs Request/Response
- Arquitectura MVC por capas

El sistema gestiona:
- Usuarios
- Roles
- Permisos
- Especialidades
- Activos
- Solicitudes de soporte (ST)
- Requerimientos (RQ)
- Órdenes de trabajo (OT)
- Estados
- Aprobaciones
- Derivaciones
- Historiales y auditoría

La prioridad es producir código:
- robusto
- mantenible
- seguro
- coherente con el modelo de datos
- fácil de probar
- sin sobreingeniería innecesaria

---

# 3. REGLAS FUNDAMENTALES

Estas reglas tienen prioridad sobre cualquier implementación improvisada.

### 2.1 No modificar código existente innecesariamente

Antes de modificar una clase existente:
1. Leer la clase completa.
2. Revisar sus relaciones y dependencias.
3. Revisar las clases relacionadas.
4. utilizar Graphify si las dependencias no son evidentes;
5. Revisar el flujo existente.
6. Realizar únicamente los cambios necesarios.

Graphify es una herramienta de navegación del proyecto y no debe utilizarse como sustituto de leer el código fuente.

No reescribir clases completas si solamente es necesario modificar una parte.

No cambiar nombres, arquitectura o convenciones existentes sin una razón técnica clara.

---

### 2.2 No inventar estructura

Antes de crear una clase, método, relación o endpoint:
- revisar el código existente;
- revisar las entidades;
- revisar repositories;
- revisar DTOs;
- revisar services;
- revisar mappers;
- revisar el grafo de Graphify cuando sea útil.

No asumir que una clase o propiedad existe.

Si una decisión depende de información que no está disponible, inspeccionar el proyecto antes de implementar.

---

### 2.3 Mantener coherencia con la base de datos

Las entidades JPA deben representar correctamente el modelo relacional existente.

No modificar tablas, columnas, relaciones o nombres de BD sin indicación explícita.

Las relaciones JPA deben respetar las FK existentes.

---

# 4. ARQUITECTURA

Usar arquitectura por capas:

Controller
    ↓
Service
    ↓
Repository
    ↓
Database

Los DTOs separan la API de las entidades JPA.

Estructura esperada:

src/main/java/cfbd/co/sgt/

├── controller/
├── dto/
│   ├── request/
│   └── response/
├── mapper/
├── model/
├── repository/
├── service/
│   └── impl/
├── exception/
└── config/
└── security/

No mezclar responsabilidades entre capas.

---

# 5. ENTIDADES JPA

Las clases de `model` representan entidades persistentes.

Reglas:

- Usar `@Entity`.
- Usar `@Table` cuando corresponda.
- Mantener las FK como relaciones JPA (`@ManyToOne`, `@OneToMany`, etc.).
- Evitar representar una FK como `String` cuando ya existe una relación JPA.
- Las relaciones `@ManyToOne` deben utilizar `FetchType.LAZY` salvo que exista una razón explícita para otra estrategia.
- Las colecciones `@OneToMany` deben utilizar `FetchType.LAZY`.
- No usar `CascadeType.ALL` automáticamente.
- No usar `orphanRemoval = true` sin justificarlo.
- No crear relaciones bidireccionales innecesarias.

## UUID

Los identificadores del sistema utilizan UUID.

# 6. SEGURIDAD Y AUTORIZACIÓN

La seguridad es un requisito obligatorio del sistema.

Todos los endpoints deben estar protegidos según corresponda mediante autenticación JWT y autorización basada en permisos.

No crear endpoints públicos por defecto.

---

## 5.1 AUTENTICACIÓN JWT

El sistema utiliza JWT para autenticación.

Flujo general:

Cliente
    ↓
Login
    ↓
Backend valida credenciales
    ↓
Genera JWT
    ↓
Cliente envía:
Authorization: Bearer <token>
    ↓
Backend valida JWT
    ↓
Obtiene identidad y permisos del usuario
    ↓
Autoriza la operación

Reglas:
- No utilizar sesiones HTTP para la autenticación principal.
- No almacenar JWT en la base de datos salvo que exista un requisito explícito.
- Validar firma, expiración y claims relevantes del JWT.
- Nunca aceptar un JWT expirado.
- Nunca confiar en información enviada por el cliente fuera del JWT validado.
- No almacenar secretos JWT directamente en el código fuente.
- La clave/secreto JWT debe provenir de variables de entorno.
- No registrar JWT completos en logs.
- No registrar passwords ni credenciales.
- El algoritmo y configuración criptográfica deben ser explícitos y seguros.
- El usuario autenticado debe obtenerse desde el contexto de seguridad de Spring, no desde un `id_usuario` enviado libremente por el cliente.

---

## 5.2 AUTORIZACIÓN BASADA EN PERMISOS

La autorización del sistema se basa en permisos asociados a roles.

Relación:

Usuario
    ↓
Rol
    ↓
Permisos

Ejemplo:

Usuario
    ↓
Rol: DESARROLLO
    ↓
solicitud.read
solicitud.create
solicitud.update

Los permisos utilizan una nomenclatura:

<recurso>.<acción>

Ejemplos:
solicitud.read
solicitud.create
solicitud.update
solicitud.delete

requerimiento.read
requerimiento.create
requerimiento.update
requerimiento.delete

orden.read
orden.create
orden.update
orden.delete

usuario.read
usuario.create
usuario.update
usuario.delete

No utilizar solamente roles para decidir autorización cuando exista un permiso específico para la operación.

---

## 5.3 TODOS LOS ENDPOINTS DEBEN VALIDAR PERMISOS

Todo endpoint protegido debe verificar el permiso correspondiente antes de ejecutar la operación.

Ejemplo:

GET /solicitudes

requiere:

solicitud.read

POST /solicitudes

requiere:

solicitud.create

PUT /solicitudes/{id}

requiere:

solicitud.update

DELETE /solicitudes/{id}

requiere:

solicitud.delete

No implementar un endpoint CRUD sin definir qué permiso requiere.

Si un endpoint realiza una operación que no corresponde exactamente a CRUD, utilizar un permiso específico cuando sea necesario.

Ejemplo:

POST /solicitudes/{id}/derivar

podría requerir:

solicitud.derive

No reutilizar automáticamente `solicitud.update` para operaciones de negocio especiales si eso permite permisos excesivamente amplios.

---

## 5.4 SPRING SECURITY

Utilizar Spring Security como mecanismo central de autenticación y autorización.

Preferir autorización declarativa cuando sea posible.

Ejemplo:

@PreAuthorize("hasAuthority('solicitud.read')")

La configuración de seguridad debe centralizarse.

No implementar verificaciones manuales repetidas dentro de cada Controller como:

if (usuarioTienePermiso(...))

si la misma regla puede resolverse mediante Spring Security.

La lógica de autorización compleja que dependa del recurso puede implementarse en un componente/service específico.

---

## 5.5 AUTORIZACIÓN A NIVEL DE RECURSO

Diferenciar:

1. Permiso para realizar una operación.
2. Permiso para acceder a un recurso específico.

Ejemplo:

Tener:

solicitud.read

no implica automáticamente que cualquier usuario pueda consultar cualquier solicitud.

Si una regla de negocio establece restricciones adicionales, deben validarse.

Ejemplo:

- un usuario puede consultar solamente sus propias solicitudes;
- un supervisor puede consultar solicitudes de su especialidad;
- un administrador puede consultar todas.

Estas reglas deben implementarse en la capa de negocio/autorización correspondiente.

No confiar únicamente en ocultar botones en el frontend.

El backend siempre debe validar la autorización.

---

## 5.6 RATE LIMITING

Todos los endpoints HTTP deben estar protegidos mediante rate limiting.

El objetivo es limitar la cantidad de solicitudes que un cliente puede realizar por minuto.

No crear endpoints sin rate limit.

El límite debe poder configurarse mediante variables de entorno.

Ejemplo:

RATE_LIMIT_REQUESTS_PER_MINUTE=60

No hardcodear el límite directamente en múltiples Controllers.

La implementación debe ser centralizada.

Preferir aplicar el rate limiting mediante:
- filtro;
- interceptor;
- middleware;
- componente de seguridad;

según la arquitectura utilizada.

No implementar contadores independientes dentro de cada Controller.

---

## 5.7 RATE LIMIT SEGÚN TIPO DE OPERACIÓN

Si el proyecto posteriormente requiere diferentes límites, permitir configuración por tipo de endpoint.

Ejemplo conceptual:
API_RATE_LIMIT_PER_MINUTE=60
AUTH_RATE_LIMIT_PER_MINUTE=10

Los endpoints de autenticación/login deben tener especial protección contra ataques de fuerza bruta.

No asumir que el rate limit general es suficiente para `/auth/login`.

---

# 7. VARIABLES DE ENTORNO
Para todas tus pruebas debe usar las variables de entorno definidas en el archivo .env
Si es necesario manejar una nueva variable de entorno se debe agregar en el .env


# 8. FRONTEND — ANGULAR

El frontend del SGT se desarrollará con Angular.
El frontend debe consumir exclusivamente las APIs REST del backend existente.
La implementación debe respetar la arquitectura y contratos existentes del backend.

## 7.1 PRINCIPIOS GENERALES
- Priorizar una interfaz profesional, moderna, limpia y consistente.
- El frontend debe parecer una aplicación empresarial real, no una colección de CRUDs.
- Priorizar UX sobre cantidad de elementos visuales.
- Evitar diseños genéricos o excesivamente ornamentados.
- Mantener una jerarquía visual clara.
- Mantener consistencia entre todas las pantallas.
- No duplicar lógica innecesariamente.
- No modificar el backend para adaptarlo al frontend sin una razón justificada.
- Antes de consumir un endpoint, revisar su contrato real.
- No inventar endpoints, campos ni respuestas.
- Utilizar Graphify como herramienta de apoyo para analizar dependencias,
  estructura y relaciones cuando la complejidad de la tarea lo justifique.
- Utilizar `ui-ux-pro-max-skill` para decisiones de diseño visual y UX
  cuando esté disponible y sea relevante.
- Utilizar `impeccable` para revisión, refinamiento y mejora visual
  cuando esté disponible y sea relevante.

## 7.2 ARQUITECTURA ANGULAR
Utilizar una arquitectura modular y mantenible.

Separar como mínimo:
- core/
- shared/
- presentation/

Ejemplo:

src/app/frontend/
├── core/
│   ├── auth/
│   ├── guards/
│   ├── interceptors/
│   ├── services/
│   └── models/
├── shared/
│   ├── components/
│   ├── directives/
│   └── pipes/
└── presentation/
      ├── views/
        ├── dashboard/
        ├── solicitudes/
        ├── requerimientos/
        ├── ordenes/
        ├── usuarios/
        ├── roles/
        ├── permisos/
        ├── activos/
        ├── estados/
        └── ...