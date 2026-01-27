import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SECTIONS, DataSetKey } from './sections.config';

import { Subfleet, SubfleetReport } from '../../../interfaces/subfleet';
import { Vehicle } from '../../../interfaces/vehicle';
import { GpsDevice, GpsHistory } from '../../../interfaces/gpsDevice';

import { SubfleetService } from '../../../services/subfleet.service';
import { TripService } from '../../../services/trip.service';
import { ExpenseService } from '../../../services/expense.service';
import { GpsDeviceService } from '../../../services/gps-device.service';
import { alertError } from '../../../utils/custom-alerts';

import {
  BaseListComponent,
  ColumnConfig,
} from '../../../components/base-list-component/base-list-component';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';
import { MapComponent } from '../../../components/map-component/map-component';
import { createDataLoader, DataLoader, DataSet } from './data-loader';

@Component({
  selector: 'app-details-subfleet',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    BaseListComponent,
    PaginatorComponent,
    MapComponent,
  ],
  templateUrl: './details-subfleet.html',
})
export class DetailsSubfleet {
  private route = inject(ActivatedRoute);
  private subfleetService = inject(SubfleetService);
  private tripService = inject(TripService);
  private expenseService = inject(ExpenseService);
  private gpsService = inject(GpsDeviceService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  subfleet?: Subfleet;
  report?: SubfleetReport;
  loading = false;

  // Veículos existentes
  vehicles: Vehicle[] = [];
  vehiclesColumns: ColumnConfig<any>[] = [
    { key: 'plate', label: 'Placa', sortable: true },
    { key: 'model', label: 'Modelo' },
    { key: 'brand', label: 'Marca' },
    { key: 'modelYear', label: 'Ano Modelo' },
  ];
  vehiclesPage = 1;
  vehiclesLimit = 5;
  vehiclesTotal = 0;
  vehiclesTotalPages = 1;

  // Dados para seções e mapa
  markers: GpsDevice[] = [];
  mapPoints: GpsHistory[] = [];
  startDate = '';
  endDate = '';
  dataSets: Record<DataSetKey, DataSet<any>> = {
    gpsEvents: { items: [], page: 1, limit: 10, total: 0, totalPages: 0 },
    trips: { items: [], page: 1, limit: 10, total: 0, totalPages: 0 },
    expenses: { items: [], page: 1, limit: 10, total: 0, totalPages: 0 },
  };

  sections = SECTIONS;
  dataLoader!: DataLoader;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadSubfleet(id);
    } else {
      this.router.navigate(['/frotas']);
    }
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

  getDataSet<K extends DataSetKey>(key: K): DataSet<any> {
    return this.dataSets[key];
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

  applyDateFilter() {
    if (!this.subfleet?.id) return;
    if (this.startDate && this.endDate && this.startDate > this.endDate) {
      alertError('A data inicial não pode ser maior que a data final.');
      return;
    }
    this.dataSets.gpsEvents.page = 1;
    this.dataLoader.loadData('gpsEvents', this.subfleet.id!, 1, {
      startDate: this.startDate ? `${this.startDate}T00:00:00` : undefined,
      endDate: this.endDate ? `${this.endDate}T23:59:59` : undefined,
    });
    this.dataLoader.loadMapHistory(
      this.subfleet.id!,
      this.startDate ? `${this.startDate}T00:00:00` : undefined,
      this.endDate ? `${this.endDate}T23:59:59` : undefined
    );
  }

  clearDateFilter() {
    if (!this.subfleet?.id) return;
    this.initializeDates();
    this.dataSets.gpsEvents.page = 1;
    this.dataLoader.loadData('gpsEvents', this.subfleet.id!, 1, {
      startDate: this.startDate ? `${this.startDate}T00:00:00` : undefined,
      endDate: this.endDate ? `${this.endDate}T23:59:59` : undefined,
    });
    this.dataLoader.loadMapHistory(this.subfleet.id!);
  }

  onPageChange(type: DataSetKey, newPage: number) {
    if (!this.subfleet?.id) return;
    this.dataLoader.loadData(
      type,
      this.subfleet.id!,
      newPage,
      this.getDateFilters(type)
    );
  }

  onVehiclesPageChange(newPage: number) {
    this.vehiclesPage = newPage;
    if (this.subfleet?.id) this.loadVehicles(this.subfleet.id);
  }

  onVehicleSelect(vehicle: any) {
    this.router.navigate(['/veiculos', vehicle.id]);
  }

  goBack() {
    this.router.navigate(['/frotas']);
  }

  private loadSubfleet(id: number) {
    this.loading = true;
    this.subfleetService.getById(id).subscribe({
      next: (res) => {
        this.subfleet = res;
        this.loading = false;
        this.cdr.detectChanges();

        // Inicializa dataLoader
        this.dataLoader = createDataLoader(
          this.subfleetService,
          this.tripService,
          this.expenseService,
          this.gpsService,
          this.cdr,
          this.dataSets,
          this.markers,
          this.mapPoints
        );

        this.loadReport(id);
        this.loadVehicles(id);
        this.initializeDates();

        ['gpsEvents', 'trips', 'expenses'].forEach((key) => {
          this.dataLoader.loadData(
            key as DataSetKey,
            id,
            undefined,
            this.getDateFilters(key as DataSetKey)
          );
        });
        this.dataLoader.loadMarkers(id);
        this.dataLoader.loadMapHistory(id);
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/frotas']);
      },
    });
  }

  private loadReport(subfleetId: number) {
    this.subfleetService.getReport(subfleetId).subscribe({
      next: (res) => {
        this.report = res;
        this.cdr.detectChanges();
      },
      error: () => {
        this.report = undefined;
        this.cdr.detectChanges();
      },
    });
  }

  private loadVehicles(subfleetId: number) {
    this.subfleetService
      .getVehiclesBySubfleet(subfleetId, this.vehiclesPage, this.vehiclesLimit)
      .subscribe({
        next: (res) => {
          this.vehicles = res.data;
          this.vehiclesTotal = res.total;
          this.vehiclesPage = res.page;
          this.vehiclesLimit = res.limit;
          this.vehiclesTotalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: () => {
          this.vehicles = [];
          this.vehiclesTotal = 0;
          this.cdr.detectChanges();
        },
      });
  }
}
