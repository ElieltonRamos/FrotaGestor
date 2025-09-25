import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-menu',
  imports: [RouterLink],
  templateUrl: './menu.html',
})
export class Menu {
  menus = [
    { name: 'Configurações', icon: 'settings', route: '/configuracoes' },
    { name: 'Veículos', icon: 'directions_car', route: '/veiculos' },
    { name: 'Motoristas', icon: 'person', route: '/motoristas' },
    { name: 'Manutenções', icon: 'build', route: '/manutencoes' },
    { name: 'Abastecimento', icon: 'local_gas_station', route: '/abastecimento' },
    { name: 'Financeiro', icon: 'attach_money', route: '/financeiro' },
    { name: 'Relatórios', icon: 'bar_chart', route: '/relatorios' },
    { name: 'Segurança', icon: 'security', route: '/seguranca' },
    { name: 'Viagens', icon: 'local_shipping', route: '/viagens' }
  ];
}
