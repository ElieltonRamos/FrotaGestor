import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Menu } from './pages/menu/menu';

export const routes: Routes = [
  {
    path: '',
    component: Login,
  },
  {
    path: 'menu',
    component: Menu,
  }
];
