import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Trip, TripIndicators, TripStatus } from '../../../interfaces/trip';
import { TripService } from '../../../services/trip.service';
import { PaginatedResponse } from '../../../interfaces/paginator';
import { Router } from '@angular/router';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { CommonModule } from '@angular/common';
import {
  BaseListComponent,
  ColumnConfig,
} from '../../../components/base-list-component/base-list-component';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';
import {
  BaseFilterComponent,
  FilterConfig,
} from '../../../components/base-filter-component/base-filter-component';
import { ModalEditComponent } from '../../../components/modal-edit-component/modal-edit-component';

@Component({
  selector: 'app-list-trip',
  imports: [
    FormsModule,
    CommonModule,
    BaseListComponent,
    PaginatorComponent,
    BaseFilterComponent,
    ModalEditComponent,
  ],
  templateUrl: './list-trip.html',
})
export class ListTrip {
  private serviceTrip = inject(TripService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  tripColumns: ColumnConfig<Trip>[] = [
    { key: 'id' as keyof Trip, label: 'ID', sortable: true },
    { key: 'vehiclePlate' as keyof Trip, label: 'Veículo', sortable: true },
    { key: 'driverName' as keyof Trip, label: 'Motorista', sortable: true },
    { key: 'startLocation' as keyof Trip, label: 'Origem', sortable: true },
    { key: 'endLocation' as keyof Trip, label: 'Destino', sortable: true },
    {
      key: 'startTime' as keyof Trip,
      label: 'Início',
      type: 'date',
      sortable: true,
    },
    {
      key: 'endTime' as keyof Trip,
      label: 'Fim',
      type: 'date',
      sortable: true,
    },
    {
      key: 'distanceKm' as keyof Trip,
      label: 'Distância (Km)',
      sortable: true,
    },
    {
      key: 'status' as keyof Trip,
      label: 'Status',
      type: 'status',
      sortable: true,
    },
  ];

  tripFilters = [
    { key: 'id', label: 'ID', type: 'number', placeholder: 'ID...' },
    {
      key: 'vehiclePlate',
      label: 'Veículo',
      type: 'text',
      placeholder: 'Placa do veículo...',
    },
    {
      key: 'driverName',
      label: 'Motorista',
      type: 'text',
      placeholder: 'Motorista...',
    },
    {
      key: 'startLocation',
      label: 'Origem',
      type: 'text',
      placeholder: 'Local de início...',
    },
    {
      key: 'endLocation',
      label: 'Destino',
      type: 'text',
      placeholder: 'Local de destino...',
    },
    { key: 'startDate', label: 'Data Inicial', type: 'date' },
    { key: 'endDate', label: 'Data Final', type: 'date' },
    {
      key: 'status',
      label: 'Status',
      type: 'select',
      options: Object.values(TripStatus),
    },
  ] satisfies FilterConfig[];

  tripFields = [
    { name: 'vehiclePlate', label: 'Placa do Veículo', type: 'text' },
    { name: 'driverName', label: 'Motorista', type: 'text' },
    { name: 'startLocation', label: 'Origem', type: 'text' },
    { name: 'endLocation', label: 'Destino', type: 'text' },
    { name: 'startTime', label: 'Data/Hora Início', type: 'datetime-local' },
    { name: 'endTime', label: 'Data/Hora Fim', type: 'datetime-local' },
    { name: 'distanceKm', label: 'Distância (Km)', type: 'number' },
    {
      name: 'status',
      label: 'Status',
      type: 'select',
      options: Object.values(TripStatus),
    },
  ];

  trips: Trip[] = [];
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 1;
  selectedTrip?: Trip;
  showModal = false;

  filter = {
    id: '',
    vehiclePlate: '',
    driverName: '',
    startLocation: '',
    endLocation: '',
    status: '',
    startDate: '',
    endDate: '',
  };

  sortKey: keyof Trip = 'id';
  sortAsc = true;

  indicators?: TripIndicators;
  loadingIndicators = false;

  ngOnInit() {
    this.listTrips(1, 10);
    this.loadIndicators();
  }

  loadIndicators() {
    this.loadingIndicators = true;
    let filterWithPeriod = { ...this.filter };

    if (!this.filter.startDate && !this.filter.endDate) {
      const now = new Date();
      const start = new Date(now.getFullYear(), now.getMonth(), 1);
      const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);

      filterWithPeriod = {
        ...this.filter,
        startDate: start.toISOString().split('T')[0],
        endDate: end.toISOString().split('T')[0],
      };
    }

    this.serviceTrip.getIndicators(filterWithPeriod).subscribe({
      next: (res) => {
        this.indicators = res;
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.indicators = {
          totalTrips: 0,
          inProgress: 0,
          completed: 0,
          canceled: 0,
          totalDistance: 0,
          avgDistance: 0,
          lastTrip: { date: '', driverName: '', vehiclePlate: '' },
        };
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
    });
  }

  listTrips(page: number, limit: number) {
    this.serviceTrip
      .getAll(page, limit, this.filter, this.sortKey, this.sortAsc)
      .subscribe({
        next: (res: PaginatedResponse<Trip>) => {
          this.trips = res.data;
          this.total = res.total;
          this.page = res.page;
          this.limit = res.limit;
          this.totalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.trips = [];
          this.total = 0;
          this.totalPages = 0;
        },
      });
  }

  applyFilters() {
    this.page = 1;
    this.listTrips(this.page, this.limit);
    this.loadIndicators();
    this.cdr.detectChanges();
  }

  clearFilters() {
    this.filter = {
      id: '',
      vehiclePlate: '',
      driverName: '',
      startLocation: '',
      endLocation: '',
      status: '',
      startDate: '',
      endDate: '',
    };
    this.applyFilters();
  }

  sortBy(key: keyof Trip) {
    if (this.sortKey === key) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortKey = key;
      this.sortAsc = true;
    }
    this.listTrips(this.page, this.limit);
  }

  onPageChange(newPage: number) {
    this.listTrips(newPage, this.limit);
  }

  onEdit(trip: Trip) {
    this.selectedTrip = { ...trip };
    this.showModal = true;
  }

  onCloseModal() {
    this.showModal = false;
  }

  onSaveModal(trip: Trip) {
    const id = trip.id;
    delete trip.id;

    this.serviceTrip.update(id!, trip).subscribe({
      next: () => {
        this.listTrips(1, 10);
        this.loadIndicators();
        this.showModal = false;
        this.selectedTrip = undefined;
        alertSuccess('Viagem atualizada com Sucesso');
      },
      error: (err) => {
        alertError(
          `Ocorreu um erro ao salvar a viagem. ${
            err?.error?.message || 'Erro desconhecido.'
          }`
        );
      },
    });
  }

  onNavDetails(id?: number) {
    if (!id) return;
    this.router.navigate(['/viagens', id]);
  }
}
