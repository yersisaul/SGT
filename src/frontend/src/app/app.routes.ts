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
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';

export const routes: Routes = [
  // { path: '', component: Home },
  { path: '', component: Login },
  //{ path: 'login', component: Login },

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
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];
