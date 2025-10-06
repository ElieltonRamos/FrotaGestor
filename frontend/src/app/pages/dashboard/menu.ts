import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';

@Component({
  selector: 'app-menu',
  imports: [RouterLink, NgIcon],
  templateUrl: './menu.html',
})
export class Menu {
  menus = [
    { name: 'Veículos', icon: 'heroTruckSolid', route: '/veiculos' },
    { name: 'Viagens', icon: 'heroMapSolid', route: '/viagens' },
    { name: 'Motoristas', icon: 'heroUserGroupSolid', route: '/motoristas' },
    { name: 'Manutenções', icon: 'heroWrenchScrewdriverSolid', route: '/manutencoes' },
    { name: 'Despesas', icon: 'heroBarsArrowDownSolid', route: '/despesas' },
    { name: 'Abastecimento', icon: 'heroFireSolid', route: '/abastecimentos' },
    { name: 'Relatórios', icon: 'heroChartBarSolid', route: '/relatorios' },
    { name: 'Usuarios', icon: 'heroUserSolid', route: '/usuarios' },
  ];
}
