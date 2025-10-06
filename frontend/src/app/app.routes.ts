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
import { guestGuard } from './guards/guest-guard';
import { DetailsTrip } from './pages/trip/details-trip/details-trip';
import { Trip } from './pages/trip/trip';
import { Expenses } from './pages/expenses/expenses';
import { DetailsExpense } from './pages/expenses/details-expense/details-expense';
import { Refueling } from './pages/refueling/refueling';
import { DetailsRefueling } from './pages/refueling/details-refueling/details-refueling';
import { Maintenance } from './pages/maintenance/maintenance';
import { DetailsMaintenance } from './pages/maintenance/details-maintenance/details-maintenance';
import { Users } from './pages/users/users';
import { DetailsUsers } from './pages/users/details-users/details-users';

export const routes: Routes = [
  {
    path: '',
    component: Login,
    canActivate: [guestGuard],
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
    path: 'usuarios',
    component: Users,
    canActivate: [authGuard],
  },
  {
    path: 'usuarios/:id',
    component: DetailsUsers,
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
    path: 'viagens',
    component: Trip,
    canActivate: [authGuard],
  },
  {
    path: 'viagens/:id',
    component: DetailsTrip,
    canActivate: [authGuard],
  },
  {
    path: 'alterar-senha',
    component: ChangePassword,
    canActivate: [authGuard],
  },
  {
    path: 'despesas',
    component: Expenses,
    canActivate: [authGuard],
  },
  {
    path: 'despesas/:id',
    component: DetailsExpense,
    canActivate: [authGuard],
  },
  {
    path: 'abastecimentos',
    component: Refueling,
    canActivate: [authGuard],
  },
  {
    path: 'abastecimentos/:id',
    component: DetailsRefueling,
    canActivate: [authGuard],
  },
  {
    path: 'manutencoes',
    component: Maintenance,
    canActivate: [authGuard],
  },
  {
    path: 'manutencoes/:id',
    component: DetailsMaintenance,
    canActivate: [authGuard],
  },
  { path: '**', component: NotFound },
];
