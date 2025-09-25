import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Trip, TripStatus } from '../../../interfaces/trip';
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

  // Configuração das colunas
  tripColumns: ColumnConfig<Trip>[] = [
    { key: 'id' as keyof Trip, label: 'ID', sortable: true },
    { key: 'vehicleId' as keyof Trip, label: 'Veículo', sortable: true },
    { key: 'driverId' as keyof Trip, label: 'Motorista', sortable: true },
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

  // Configuração dos filtros
  tripFilters = [
    { key: 'id', label: 'ID', type: 'number', placeholder: 'ID...' },
    {
      key: 'vehicleId',
      label: 'Veículo',
      type: 'number',
      placeholder: 'ID veículo...',
    },
    {
      key: 'driverId',
      label: 'Motorista',
      type: 'number',
      placeholder: 'ID motorista...',
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
    {
      key: 'status',
      label: 'Status',
      type: 'select',
      options: Object.values(TripStatus),
    },
  ] satisfies FilterConfig[];

  tripFields = [
    { name: 'vehicleId', label: 'ID do Veículo', type: 'number' },
    { name: 'driverId', label: 'ID do Motorista', type: 'number' },
    { name: 'startLocation', label: 'Origem', type: 'text' },
    { name: 'endLocation', label: 'Destino', type: 'text' },
    { name: 'startTime', label: 'Data/Hora Início', type: 'datetime-local' },
    { name: 'endTime', label: 'Data/Hora Fim', type: 'datetime-local' },
    { name: 'distanceKm', label: 'Distância (Km)', type: 'number' },
    {
      name: 'status',
      label: 'Status',
      type: 'select',
      options: ['PLANEJADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA'],
    },
  ];

  trips: Trip[] = [];
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 1;
  selectedTrip?: Trip;
  showModal = false;

  // filtros
  filter = {
    id: '',
    vehicleId: '',
    driverId: '',
    startLocation: '',
    endLocation: '',
    status: '',
  };

  // ordenação
  sortKey: keyof Trip = 'id';
  sortAsc = true;

  ngOnInit() {
    this.listTrips(1, 10);
  }

  listTrips(page: number, limit: number) {
    console.log('Listando viagens...', this.filter);
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
          console.log('Erro ao carregar viagens:', err);
          this.trips = [];
          this.total = 0;
          this.totalPages = 0;
        },
      });
  }

  applyFilters() {
    this.page = 1;
    this.listTrips(this.page, this.limit);
    this.cdr.detectChanges();
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

  clearFilters() {
    this.filter = {
      id: '',
      vehicleId: '',
      driverId: '',
      startLocation: '',
      endLocation: '',
      status: '',
    };
    this.applyFilters();
  }

  onPageChange(newPage: number) {
    this.listTrips(newPage, this.limit);
  }

  onEdit(trip: Trip) {
    console.log('Editando viagem:', trip);
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
