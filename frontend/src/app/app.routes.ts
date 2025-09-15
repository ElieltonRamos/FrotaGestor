import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Menu } from './pages/menu/menu';
import { Motorista } from './pages/motorista/motorista';

export const routes: Routes = [
  {
    path: '',
    component: Login,
  },
  {
    path: 'menu',
    component: Menu,
  },
  {
    path: 'motoristas',
    component: Motorista
  }
];
