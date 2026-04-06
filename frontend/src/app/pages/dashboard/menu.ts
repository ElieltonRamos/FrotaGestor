import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { DatePipe } from '@angular/common';
import { MapComponent } from '../../components/map-component/map-component';
import { GpsDeviceService } from '../../services/gps-device.service';
import { GpsDevice } from '../../interfaces/gpsDevice';
import { alertError } from '../../utils/custom-alerts';

@Component({
  selector: 'app-menu',
  imports: [RouterLink, NgIcon, MapComponent, DatePipe],
  templateUrl: './menu.html',
})
export class Menu {
  private gpsDeviceService = inject(GpsDeviceService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  menus = [
    { name: 'Veículos', icon: 'heroTruckSolid', route: '/veiculos' },
    { name: 'Frotas', icon: 'heroTableCellsSolid', route: '/frotas' },
    {
      name: 'Dispositivos',
      icon: 'heroDeviceTabletSolid',
      route: '/dispositivos',
    },
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
  devicesWithoutPower: GpsDevice[] = [];

  ngOnInit(): void {
    this.gpsDeviceService.getAll().subscribe({
      next: (response) => {
        this.markers = response.data;
        this.cdr.detectChanges();
      },
      error: (err) =>
        alertError(`Erro ao buscar Dispositivos GPS ${err.error.message}`),
    });

    this.gpsDeviceService.getDevicesWithoutPower().subscribe({
      next: (response) => {
        this.devicesWithoutPower = response;
        this.cdr.detectChanges();
      },
      error: (err) =>
        alertError(
          `Erro ao buscar dispositivos sem conexão: ${err.error.message}`,
        ),
    });
  }

  onNavDetails(id: number): void {
    this.router.navigate(['/dispositivos', id]);
  }
}
