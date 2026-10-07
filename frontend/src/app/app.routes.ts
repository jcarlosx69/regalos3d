import { Routes } from '@angular/router';
import { requiereSesion } from './core/auth';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Entrar - Regalos 3D',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [requiereSesion],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'encargos' },
      {
        path: 'encargos',
        title: 'Encargos - Regalos 3D',
        loadComponent: () => import('./pages/encargos/encargos-lista').then((m) => m.EncargosLista),
      },
      {
        path: 'encargos/nuevo',
        title: 'Nuevo encargo - Regalos 3D',
        loadComponent: () => import('./pages/encargos/encargo-form').then((m) => m.EncargoForm),
      },
      {
        path: 'encargos/:id',
        title: 'Encargo - Regalos 3D',
        loadComponent: () => import('./pages/encargos/encargo-form').then((m) => m.EncargoForm),
      },
      {
        path: 'clientes',
        title: 'Clientes - Regalos 3D',
        loadComponent: () => import('./pages/clientes/clientes-lista').then((m) => m.ClientesLista),
      },
      {
        path: 'clientes/:id',
        title: 'Cliente - Regalos 3D',
        loadComponent: () => import('./pages/clientes/cliente-detalle').then((m) => m.ClienteDetalle),
      },
      {
        path: 'modelos',
        title: 'Modelos - Regalos 3D',
        loadComponent: () => import('./pages/modelos/modelos-lista').then((m) => m.ModelosLista),
      },
      {
        path: 'informes',
        title: 'Informes - Regalos 3D',
        loadComponent: () => import('./pages/informes/informes').then((m) => m.Informes),
      },
      {
        path: 'ajustes',
        title: 'Ajustes - Regalos 3D',
        loadComponent: () => import('./pages/ajustes/ajustes').then((m) => m.Ajustes),
      },
      // Direcciones de la versión anterior (marcadores guardados)
      { path: 'regalos', redirectTo: 'encargos' },
      { path: 'regalos/nuevo', redirectTo: 'encargos/nuevo' },
      { path: 'personas', redirectTo: 'clientes' },
    ],
  },
  { path: '**', redirectTo: '' },
];
