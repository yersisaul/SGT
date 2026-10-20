import { Routes } from '@angular/router';
import { Home } from './presentation/views/home/home';
import { Login } from './presentation/views/auth/login/login';
import { AppShell } from './presentation/layout/app-shell/app-shell';
import { Dashboard } from './presentation/views/dashboard/dashboard';
import { Solicitudes } from './presentation/views/solicitudes/solicitudes';
import { Requerimientos } from './presentation/views/requerimientos/requerimientos';
import { Ordenes } from './presentation/views/ordenes/ordenes';
import { Usuarios } from './presentation/views/administracion/usuarios/usuarios';
import { Roles } from './presentation/views/administracion/roles/roles';
import { Permisos } from './presentation/views/administracion/permisos/permisos';
import { Activos } from './presentation/views/administracion/activos/activos';
import { Especialidades } from './presentation/views/administracion/especialidades/especialidades';
import { Estados } from './presentation/views/administracion/estados/estados';
import { Sla } from './presentation/views/administracion/sla/sla';
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';
import { webGuard, webPermissionGuard } from './core/guards/web.guard';

export const routes: Routes = [
  // { path: '', component: Home },
  { path: '', component: Login },
  //{ path: 'login', component: Login },

  // Panel de administración del sitio web público (www.cfbd.co): login y
  // menú propios; reutiliza la sesión JWT y el diseño del SGT. Carga diferida
  // para no engordar el bundle inicial del SGT.
  { path: 'web/login', component: Login, data: { contexto: 'web' } },
  {
    path: 'web',
    loadComponent: () => import('./presentation/layout/web-shell/web-shell').then((m) => m.WebShell),
    canActivate: [webGuard],
    children: [
      {
        path: 'resumen',
        loadComponent: () => import('./presentation/views/web/resumen/resumen').then((m) => m.Resumen),
      },
      {
        path: 'productos',
        loadComponent: () => import('./presentation/views/web/productos/productos').then((m) => m.Productos),
        canActivate: [webPermissionGuard],
        data: { permission: 'web_catalogo.read' },
      },
      {
        path: 'servicios',
        loadComponent: () => import('./presentation/views/web/servicios/servicios').then((m) => m.Servicios),
        canActivate: [webPermissionGuard],
        data: { permission: 'web_catalogo.read' },
      },
      {
        path: 'casos-exito',
        loadComponent: () => import('./presentation/views/web/casos-exito/casos-exito').then((m) => m.CasosExito),
        canActivate: [webPermissionGuard],
        data: { permission: 'web_comercial.read' },
      },
      {
        path: 'clientes',
        loadComponent: () => import('./presentation/views/web/clientes/clientes').then((m) => m.Clientes),
        canActivate: [webPermissionGuard],
        data: { permission: 'web_comercial.read' },
      },
      {
        path: 'partners',
        loadComponent: () => import('./presentation/views/web/partners/partners').then((m) => m.Partners),
        canActivate: [webPermissionGuard],
        data: { permission: 'web_comercial.read' },
      },
      {
        path: 'novedades',
        loadComponent: () => import('./presentation/views/web/novedades/novedades').then((m) => m.Novedades),
        canActivate: [webPermissionGuard],
        data: { permission: 'web_novedad.read' },
      },
      {
        path: 'documentos',
        loadComponent: () => import('./presentation/views/web/documentos/documentos').then((m) => m.Documentos),
        canActivate: [webPermissionGuard],
        data: { permission: 'web_documento.read' },
      },
      {
        path: 'registros',
        loadComponent: () => import('./presentation/views/web/registros/registros').then((m) => m.Registros),
        canActivate: [webPermissionGuard],
        data: { permission: 'usuario.update' },
      },
      { path: '', redirectTo: 'resumen', pathMatch: 'full' },
    ],
  },
  {
    path: 'app',
    component: AppShell,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: Dashboard },
      {
        path: 'solicitudes',
        component: Solicitudes,
        canActivate: [permissionGuard],
        data: { permission: 'solicitud.read' },
      },
      {
        path: 'requerimientos',
        component: Requerimientos,
        canActivate: [permissionGuard],
        data: { permission: 'requerimiento.read' },
      },
      {
        path: 'ordenes',
        component: Ordenes,
        canActivate: [permissionGuard],
        data: { permission: 'orden.read' },
      },
      {
        // usuario.read ahora también lo tienen Cliente/Despachador (para
        // resolver nombres de usuario en Solicitud/Requerimiento/Orden), así
        // que ya no sirve para distinguir quién administra usuarios. La
        // pantalla de administración se gatea con usuario.create, que solo
        // tiene Administrador.
        path: 'administracion/usuarios',
        component: Usuarios,
        canActivate: [permissionGuard],
        data: { permission: 'usuario.create' },
      },
      {
        path: 'administracion/roles',
        component: Roles,
        canActivate: [permissionGuard],
        data: { permission: 'rol.read' },
      },
      {
        path: 'administracion/permisos',
        component: Permisos,
        canActivate: [permissionGuard],
        data: { permission: 'permiso.read' },
      },
      {
        path: 'administracion/activos',
        component: Activos,
        canActivate: [permissionGuard],
        data: { permission: 'activo.read' },
      },
      {
        path: 'administracion/especialidades',
        component: Especialidades,
        canActivate: [permissionGuard],
        data: { permission: 'especialidad.read' },
      },
      {
        path: 'administracion/estados',
        component: Estados,
        canActivate: [permissionGuard],
        data: { permission: 'estado.read' },
      },
      {
        path: 'administracion/sla',
        component: Sla,
        canActivate: [permissionGuard],
        data: { permission: 'sla.read' },
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];
