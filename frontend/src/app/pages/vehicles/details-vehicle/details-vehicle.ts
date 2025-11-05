import { ChangeDetectorRef, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';

import { VehicleService } from '../../../services/vehicle.service';
import { GpsDeviceService } from '../../../services/gps-device.service';
import { MapComponent } from '../../../components/map-component/map-component';
import { BaseListComponent } from '../../../components/base-list-component/base-list-component';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';

import { Vehicle, VehicleStatus } from '../../../interfaces/vehicle';
import { Driver } from '../../../interfaces/driver';
import { GpsDevice } from '../../../interfaces/gpsDevice';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

import { createDataLoader, DataSet } from './data-loader';
import { SECTIONS, DataSetKey } from './sections.config';

@Component({
  selector: 'app-details-vehicle',
  standalone: true,
  imports: [CommonModule, MapComponent, BaseListComponent, PaginatorComponent],
  templateUrl: './details-vehicle.html',
})
export class DetailsVehicle {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private serviceVehicle = inject(VehicleService);
  private serviceGpsDevice = inject(GpsDeviceService);

  vehicle?: Vehicle;
  topDriver?: Driver;
  gpsDevice = signal<GpsDevice | null>(null);
  markers: GpsDevice[] = [];
  loading = false;

  dataSets: Record<DataSetKey, DataSet<any>> = {
    gpsEvents: { items: [], page: 1, limit: 10, total: 0, totalPages: 0 },
    trips: { items: [], page: 1, limit: 10, total: 0, totalPages: 0 },
    expenses: { items: [], page: 1, limit: 10, total: 0, totalPages: 0 },
  };

  sections = SECTIONS;
  loadData = createDataLoader(
    this.serviceVehicle,
    this.serviceGpsDevice,
    this.cdr,
    this.dataSets
  );

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) this.router.navigate(['/veiculos']);
    this.loadAll(id);
  }

  getDataSet<K extends DataSetKey>(key: K) {
    return this.dataSets[key];
  }

  private loadAll(id: number) {
    this.loading = true;
    this.serviceVehicle.getById(id).subscribe({
      next: (res) => {
        this.vehicle = res;
        this.loading = false;
        this.loadRelated(id);
      },
      error: () => this.router.navigate(['/veiculos']),
    });
  }

  private loadRelated(vehicleId: number) {
    this.serviceVehicle
      .getTopDriverByVehicle(vehicleId)
      .subscribe({ next: (d) => (this.topDriver = d) });

    this.serviceGpsDevice.getGpsDeviceByVehicle(vehicleId).subscribe({
      next: (res) => {
        this.gpsDevice.set(res);
        this.markers = [res];
      },
      error: (e) => alertError(`Erro ao buscar GPS: ${e.error.message}`),
    });

    (Object.keys(this.dataSets) as DataSetKey[]).forEach((key) =>
      this.loadData(key, vehicleId)
    );
  }

  onPageChange(type: DataSetKey, newPage: number) {
    this.dataSets[type].page = newPage;
  }

  toggleStatus(active: boolean) {
    if (!this.vehicle?.id) return;
    const status = active ? VehicleStatus.ATIVO : VehicleStatus.INATIVO;
    const message = active
      ? 'Veículo reativado com sucesso.'
      : 'Veículo desabilitado com sucesso.';

    this.loading = true;
    this.serviceVehicle.update(this.vehicle.id, { status }).subscribe({
      next: () => {
        this.loadAll(this.vehicle!.id!);
        alertSuccess(message);
      },
      error: (err) => {
        this.loading = false;
        alertError(`Erro ao atualizar veículo. ${err?.error?.message || ''}`);
      },
    });
  }

  goBack() {
    this.router.navigate(['/veiculos']);
  }
}
