# Graph Report - sgt  (2026-09-17)

## Corpus Check
- Corpus is ~9,570 words - fits in a single context window. You may not need a graph.

## Summary
- 506 nodes · 1221 edges · 29 communities (24 shown, 5 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 23 edges (avg confidence: 0.8)
- Token cost: 63,076 input · 0 output

## Community Hubs (Navigation)
- Shared DTO/Entity Imports
- REST Controller Layer
- Repository & Service Layer
- Angular CLI Configuration
- Angular App Bootstrap
- Activo Domain
- Orden Domain
- Angular Build Tooling
- Permiso Domain
- Requerimiento Domain
- Rol Domain
- Solicitud Domain
- Global Exception Handling
- Frontend Dependencies
- Maven Wrapper Script
- Frontend Dev Tooling
- Auth Request DTOs
- Angular SSR Server
- Security Configuration
- NPM Scripts
- Spring Boot Test
- Application Entry Point
- Frontend Scaffold Provenance
- Routed View Templates
- Auth Response DTOs
- Access Token Response
- Logout Request
- Message Response
- Project Root Package

## God Nodes (most connected - your core abstractions)
1. `Solicitud` - 31 edges
2. `Requerimiento` - 30 edges
3. `Rol` - 27 edges
4. `Orden` - 25 edges
5. `Permiso` - 25 edges
6. `Usuario` - 25 edges
7. `UsuarioResponse` - 22 edges
8. `Activo` - 22 edges
9. `Estado` - 18 edges
10. `OrdenServiceImpl` - 17 edges

## Surprising Connections (you probably didn't know these)
- `Clientes View Template` --semantically_similar_to--> `Productos View Template`  [INFERRED] [semantically similar]
  src/frontend/src/app/presentation/views/clientes/clientes.html → src/frontend/src/app/presentation/views/productos/productos.html
- `Home View Template` --semantically_similar_to--> `Clientes View Template`  [INFERRED] [semantically similar]
  src/frontend/src/app/presentation/views/home/home.html → src/frontend/src/app/presentation/views/clientes/clientes.html
- `Home View Template` --semantically_similar_to--> `Productos View Template`  [INFERRED] [semantically similar]
  src/frontend/src/app/presentation/views/home/home.html → src/frontend/src/app/presentation/views/productos/productos.html
- `Activo` --references--> `Especialidad`  [EXTRACTED]
  src/main/java/cfbd/co/sgt/model/Activo.java → src/main/java/cfbd/co/sgt/model/Especialidad.java
- `Activo` --references--> `Solicitud`  [EXTRACTED]
  src/main/java/cfbd/co/sgt/model/Activo.java → src/main/java/cfbd/co/sgt/model/Solicitud.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Router-outlet Routed Views Pattern** — src_frontend_src_app_app_routeroutlet, src_frontend_src_app_presentation_views_home_home_homecomponent, src_frontend_src_app_presentation_views_clientes_clientes_clientescomponent, src_frontend_src_app_presentation_views_productos_productos_productoscomponent [INFERRED 0.75]
- **Angular CLI Generated Scaffolding** — src_frontend_readme_angular_cli, src_frontend_src_app_app_approottemplate, src_frontend_src_index_approot, src_frontend_src_app_presentation_views_home_home_homecomponent [INFERRED 0.70]

## Communities (29 total, 5 thin omitted)

### Community 0 - "Shared DTO/Entity Imports"
Cohesion: 0.10
Nodes (46): arraylist, com.fasterxml.jackson.databind.annotation.JsonNaming, instant, lombok.AllArgsConstructor, lombok.Builder, lombok.Getter, lombok.NoArgsConstructor, lombok.Setter (+38 more)

### Community 1 - "REST Controller Layer"
Cohesion: 0.09
Nodes (24): lombok.RequiredArgsConstructor, org.springframework.http.ResponseEntity, org.springframework.security.crypto.password.PasswordEncoder, org.springframework.stereotype.Component, org.springframework.transaction.annotation.Transactional, org.springframework.web.bind.annotation.DeleteMapping, org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.PostMapping (+16 more)

### Community 2 - "Repository & Service Layer"
Cohesion: 0.14
Nodes (22): autowired, collectors, jakarta.transaction.Transactional, list, optional, org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, org.springframework.stereotype.Service (+14 more)

### Community 3 - "Angular CLI Configuration"
Cohesion: 0.05
Nodes (44): build, serve, test, builder, configurations, defaultConfiguration, options, cli (+36 more)

### Community 4 - "Angular App Bootstrap"
Cohesion: 0.10
Nodes (18): @angular/core, ref_angular_core_testing, @angular/platform-browser, @angular/router, @angular/ssr, App, appConfig, config (+10 more)

### Community 5 - "Activo Domain"
Cohesion: 0.16
Nodes (10): ActivoRequest, ActivoResponse, Activo, Entity, PropertyNamingStrategies.SnakeCaseStrategy, Table, ActivoRepository, ActivoService (+2 more)

### Community 6 - "Orden Domain"
Cohesion: 0.16
Nodes (9): OrdenRequest, OrdenResponse, Entity, PropertyNamingStrategies.SnakeCaseStrategy, Table, Orden, Override, OrdenServiceImpl (+1 more)

### Community 7 - "Angular Build Tooling"
Cohesion: 0.10
Nodes (19): @angular/build, @angular/cli, @angular/common, @angular/compiler, @angular/compiler-cli, @angular/forms, @angular/platform-server, jsdom (+11 more)

### Community 8 - "Permiso Domain"
Cohesion: 0.18
Nodes (7): Entity, PropertyNamingStrategies.SnakeCaseStrategy, Table, Permiso, Override, PermisoServiceImpl, PermisoService

### Community 9 - "Requerimiento Domain"
Cohesion: 0.18
Nodes (7): Entity, PropertyNamingStrategies.SnakeCaseStrategy, Table, Requerimiento, Override, RequerimientoServiceImpl, RequerimientoService

### Community 10 - "Rol Domain"
Cohesion: 0.18
Nodes (7): Entity, PropertyNamingStrategies.SnakeCaseStrategy, Table, Rol, Override, RolServiceImpl, RolService

### Community 11 - "Solicitud Domain"
Cohesion: 0.18
Nodes (7): Entity, PropertyNamingStrategies.SnakeCaseStrategy, Table, Solicitud, Override, SolicitudServiceImpl, SolicitudService

### Community 12 - "Global Exception Handling"
Cohesion: 0.21
Nodes (10): httpstatus, linkedhashmap, map, org.springframework.http.ProblemDetail, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.bind.MethodArgumentNotValidException, org.springframework.web.server.ResponseStatusException (+2 more)

### Community 13 - "Frontend Dependencies"
Cohesion: 0.17
Nodes (12): dependencies, @angular/common, @angular/compiler, @angular/core, @angular/forms, @angular/platform-browser, @angular/platform-server, @angular/router (+4 more)

### Community 14 - "Maven Wrapper Script"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 15 - "Frontend Dev Tooling"
Cohesion: 0.20
Nodes (10): devDependencies, @angular/build, @angular/cli, @angular/compiler-cli, jsdom, prettier, @types/express, @types/node (+2 more)

### Community 16 - "Auth Request DTOs"
Cohesion: 0.22
Nodes (6): email, notblank, size, LoginRequest, RefreshRequest, RegisterRequest

### Community 17 - "Angular SSR Server"
Cohesion: 0.25
Nodes (7): ref_angular_ssr_node, express, ref_node_path, angularApp, app, browserDistFolder, reqHandler

### Community 18 - "Security Configuration"
Cohesion: 0.43
Nodes (5): bcryptpasswordencoder, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.security.config.annotation.web.configuration.EnableWebSecurity, SecurityConfig

### Community 19 - "NPM Scripts"
Cohesion: 0.29
Nodes (7): scripts, build, ng, serve:ssr:frontend, start, test, watch

### Community 20 - "Spring Boot Test"
Cohesion: 0.60
Nodes (3): org.junit.jupiter.api.Test, org.springframework.boot.test.context.SpringBootTest, SgtApplicationTests

### Community 21 - "Application Entry Point"
Cohesion: 0.50
Nodes (3): org.springframework.boot.autoconfigure.SpringBootApplication, springapplication, SgtApplication

### Community 22 - "Frontend Scaffold Provenance"
Cohesion: 0.40
Nodes (5): Frontend README, Angular CLI, Vitest Test Runner, AppComponent Default Template (app.html), <app-root> bootstrap tag (index.html)

### Community 23 - "Routed View Templates"
Cohesion: 1.00
Nodes (4): router-outlet in app.html, Clientes View Template, Home View Template, Productos View Template

## Knowledge Gaps
- **89 isolated node(s):** `cfbd.co:sgt`, `$schema`, `version`, `packageManager`, `analytics` (+84 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 167 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Solicitud` connect `Solicitud Domain` to `Shared DTO/Entity Imports`, `Repository & Service Layer`, `Activo Domain`, `Orden Domain`?**
  _High betweenness centrality (0.039) - this node is a cross-community bridge._
- **Why does `Rol` connect `Rol Domain` to `Shared DTO/Entity Imports`, `REST Controller Layer`, `Repository & Service Layer`?**
  _High betweenness centrality (0.039) - this node is a cross-community bridge._
- **Why does `Requerimiento` connect `Requerimiento Domain` to `Shared DTO/Entity Imports`, `Repository & Service Layer`, `Orden Domain`?**
  _High betweenness centrality (0.038) - this node is a cross-community bridge._
- **What connects `cfbd.co:sgt`, `$schema`, `version` to the rest of the system?**
  _89 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Shared DTO/Entity Imports` be split into smaller, more focused modules?**
  _Cohesion score 0.0996488147497805 - nodes in this community are weakly interconnected._
- **Should `REST Controller Layer` be split into smaller, more focused modules?**
  _Cohesion score 0.0859538784067086 - nodes in this community are weakly interconnected._
- **Should `Repository & Service Layer` be split into smaller, more focused modules?**
  _Cohesion score 0.14030612244897958 - nodes in this community are weakly interconnected._