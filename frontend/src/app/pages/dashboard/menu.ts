import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-menu',
  imports: [RouterLink],
  templateUrl: './menu.html',
})
export class Menu {
  menus = [
    { name: 'Veículos', icon: 'directions_car', route: '/veiculos' },
    { name: 'Viagens', icon: 'local_shipping', route: '/viagens' },
    { name: 'Motoristas', icon: 'person', route: '/motoristas' },
    { name: 'Manutenções', icon: 'build', route: '/manutencoes' },
    { name: 'Despesas', icon: 'tire_repair', route: '/despesas' },
    { name: 'Abastecimento', icon: 'local_gas_station', route: '/abastecimento' },
    { name: 'Relatórios', icon: 'bar_chart', route: '/relatorios' },
  ];
}
