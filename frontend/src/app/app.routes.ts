import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { authGuard } from './guards/auth-guard';
import { NotFound } from './pages/not-found/not-found';
import { ChangePassword } from './pages/change-password/change-password';
import { Menu } from './pages/dashboard/menu';
import { Motorista } from './pages/driver/motorista';
import { DetailsDriver } from './pages/driver/details-driver/details-driver';
import { Vehicles } from './pages/vehicles/vehicles';
import { DetailsVehicle } from './pages/vehicles/details-vehicle/details-vehicle';

export const routes: Routes = [
  {
    path: '',
    component: Login,
  },
  {
    path: 'menu',
    component: Menu,
    canActivate: [authGuard],
  },
  {
    path: 'veiculos',
    component: Vehicles,
    canActivate: [authGuard],
  },
  {
    path: 'veiculos/:id',
    component: DetailsVehicle,
    canActivate: [authGuard],
  },
  {
    path: 'motoristas',
    component: Motorista,
    canActivate: [authGuard],
  },
  {
    path: 'motoristas/:id',
    component: DetailsDriver,
    canActivate: [authGuard],
  },
  {
    path: 'alterar-senha',
    component: ChangePassword,
    canActivate: [authGuard],
  },
  { path: '**', component: NotFound },
];
