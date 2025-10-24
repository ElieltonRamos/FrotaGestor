import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { MapComponent } from '../../components/map-component/map-component';
import { GpsDeviceService } from '../../services/gps-device.service';
import { GpsDevice } from '../../interfaces/gpsDevice';
import { alertError } from '../../utils/custom-alerts';

@Component({
  selector: 'app-menu',
  imports: [RouterLink, NgIcon, MapComponent],
  templateUrl: './menu.html',
})
export class Menu {
  private gpsDeviceService = inject(GpsDeviceService);

  menus = [
    { name: 'Veículos', icon: 'heroTruckSolid', route: '/veiculos' },
    { name: 'Viagens', icon: 'heroMapSolid', route: '/viagens' },
    { name: 'Motoristas', icon: 'heroUserGroupSolid', route: '/motoristas' },
    {
      name: 'Manutenções',
      icon: 'heroWrenchScrewdriverSolid',
      route: '/manutencoes',
    },
    { name: 'Despesas', icon: 'heroBarsArrowDownSolid', route: '/despesas' },
    { name: 'Abastecimento', icon: 'heroFireSolid', route: '/abastecimentos' },
    { name: 'Relatórios', icon: 'heroChartBarSolid', route: '/relatorios' },
    { name: 'Usuarios', icon: 'heroUserSolid', route: '/usuarios' },
  ];

  markers: GpsDevice[] = [];

  ngOnInit(): void {
    this.gpsDeviceService.getAll().subscribe({
      next: (response) => {
        this.markers = response.data;
      },
      error: (err) => {
        alertError(`Erro ao buscar Dispositivos GPS ${err.error.message}`)
      },
    });
  }
}
