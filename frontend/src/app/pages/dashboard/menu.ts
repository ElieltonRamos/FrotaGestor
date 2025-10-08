import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import {
  CustomMarker,
  MapComponent,
} from '../../components/map-component/map-component';
import { VehicleService } from '../../services/vehicle.service';

@Component({
  selector: 'app-menu',
  imports: [RouterLink, NgIcon, MapComponent],
  templateUrl: './menu.html',
})
export class Menu {
  private vehicleService = inject(VehicleService);

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

  markers: CustomMarker[] = [];

  ngOnInit(): void {
    this.vehicleService.getLocationsVehicles().subscribe({
      next: (markers) => (this.markers = markers),
    });
  }
}
