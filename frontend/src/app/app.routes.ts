import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Menu } from './pages/menu/menu';
import { Motorista } from './pages/motorista/motorista';
import { authGuard } from './guards/auth-guard';
import { NotFound } from './pages/not-found/not-found';
import { ChangePassword } from './pages/change-password/change-password';

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
    path: 'motoristas',
    component: Motorista,
    canActivate: [authGuard],
  },
  {
    path: 'alterar-senha',
    component: ChangePassword,
    canActivate: [authGuard],
  },
  { path: '**', component: NotFound },
];
