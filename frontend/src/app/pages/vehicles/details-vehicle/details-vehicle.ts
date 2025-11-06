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
import { GpsDevice, GpsHistory } from '../../../interfaces/gpsDevice';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

import { createDataLoader, DataSet } from './data-loader';
import { SECTIONS, DataSetKey, AVAILABLE_COMMANDS } from './sections.config';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-details-vehicle',
  standalone: true,
  imports: [
    FormsModule,
    CommonModule,
    MapComponent,
    BaseListComponent,
    PaginatorComponent,
  ],
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
  mapPoints: GpsHistory[] = [];

  // Filtros de data para eventos GPS
  startDate: string = '';
  endDate: string = '';

  dataSets: Record<DataSetKey, DataSet<any>> = {
    gpsEvents: { items: [], page: 1, limit: 10, total: 0, totalPages: 0 },
    trips: { items: [], page: 1, limit: 10, total: 0, totalPages: 0 },
    expenses: { items: [], page: 1, limit: 10, total: 0, totalPages: 0 },
  };

  availableCommands = AVAILABLE_COMMANDS;
  selectedCommand: string = 'StatusReq';

  sections = SECTIONS;
  loadData = createDataLoader(
    this.serviceVehicle,
    this.serviceGpsDevice,
    this.cdr,
    this.dataSets
  );

  ngOnInit() {
    this.initializeDates();
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) this.router.navigate(['/veiculos']);
    this.loadAll(id);
  }

  private initializeDates() {
    const today = new Date();
    this.endDate = this.formatDateForInput(today);
    this.startDate = this.formatDateForInput(today);
  }

  private formatDateForInput(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
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
    
    this.loadMapHistory(vehicleId);

    (Object.keys(this.dataSets) as DataSetKey[]).forEach((key) =>
      this.loadData(key, vehicleId, undefined, this.getDateFilters(key))
    );
  }

  private loadMapHistory(vehicleId: number) {
    const startDateTime = this.startDate ? `${this.startDate}T00:00:00` : undefined;
    const endDateTime = this.endDate ? `${this.endDate}T23:59:59` : undefined;
    
    this.serviceGpsDevice
      .getHistoryDevice(vehicleId, 1, 200, startDateTime, endDateTime)
      .subscribe({
        next: (res) => {
          this.mapPoints = res.data.filter(
            (p: GpsHistory) => p.latitude !== 0 && p.longitude !== 0
          );
          this.cdr.detectChanges();
        },
      });
  }

  private getDateFilters(key: DataSetKey) {
    if (key === 'gpsEvents') {
      return {
        startDate: this.startDate ? `${this.startDate}T00:00:00` : undefined,
        endDate: this.endDate ? `${this.endDate}T23:59:59` : undefined,
      };
    }
    return undefined;
  }

  onPageChange(type: DataSetKey, newPage: number) {
    if (!this.vehicle?.id) return;
    this.loadData(type, this.vehicle.id, newPage, this.getDateFilters(type));
  }

  applyDateFilter() {
    if (!this.vehicle?.id) return;
    
    if (this.startDate && this.endDate && this.startDate > this.endDate) {
      alertError('A data inicial não pode ser maior que a data final.');
      return;
    }

    this.dataSets.gpsEvents.page = 1;
    this.loadData('gpsEvents', this.vehicle.id, 1, {
      startDate: this.startDate ? `${this.startDate}T00:00:00` : undefined,
      endDate: this.endDate ? `${this.endDate}T23:59:59` : undefined,
    });
    
    // Atualiza também o mapa com o filtro de data
    this.loadMapHistory(this.vehicle.id);
  }

  clearDateFilter() {
    if (!this.vehicle?.id) return;
    this.initializeDates();
    this.dataSets.gpsEvents.page = 1;
    this.loadData('gpsEvents', this.vehicle.id, 1, {
      startDate: this.startDate ? `${this.startDate}T00:00:00` : undefined,
      endDate: this.endDate ? `${this.endDate}T23:59:59` : undefined,
    });
    
    // Atualiza também o mapa voltando para a data do dia
    this.loadMapHistory(this.vehicle.id);
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

  sendGpsCommand() {
    const gps = this.gpsDevice();
    if (!gps) {
      alertError('Nenhum dispositivo GPS vinculado.');
      return;
    }

    this.loading = true;
    const request = {
      commandType: this.selectedCommand,
      deviceId: gps.imei,
      parameters: {},
    };

    this.serviceGpsDevice.sendCommandDevice(request).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          alertSuccess(
            `Comando "${this.selectedCommand}" enviado com sucesso!`
          );
          this.loadAll(this.vehicle!.id!);
        } else {
          alertError(`Falha ao enviar comando: ${res.message}`);
        }
      },
      error: (err) => {
        this.loading = false;
        alertError(
          `Erro ao enviar comando: ${err?.error?.message || 'Desconhecido'}`
        );
      },
    });
  }

  goBack() {
    this.router.navigate(['/veiculos']);
  }
}