import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Vehicle } from '../../../interfaces/vehicle';
import { VehicleService } from '../../../services/vehicle.service';
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

@Component({
  selector: 'app-list-vehicle',
  imports: [
    FormsModule,
    CommonModule,
    BaseListComponent,
    PaginatorComponent,
    BaseFilterComponent,
  ],
  templateUrl: './list-vehicle.html',
})
export class ListVehicle {
  private serviceVehicle = inject(VehicleService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  vehicleFields = [
    { name: 'plate', label: 'Placa', type: 'text' },
    { name: 'model', label: 'Modelo', type: 'text' },
    { name: 'brand', label: 'Marca', type: 'text' },
    { name: 'year', label: 'Ano', type: 'number' },
    {
      name: 'status',
      label: 'Status',
      type: 'select',
      options: ['ATIVO', 'INATIVO', 'MANUTENCAO'],
    },
  ];

  vehicleColumns: ColumnConfig<Vehicle>[] = [
    { key: 'plate' as keyof Vehicle, label: 'Placa', sortable: true },
    { key: 'model' as keyof Vehicle, label: 'Modelo', sortable: true },
    { key: 'brand' as keyof Vehicle, label: 'Marca', sortable: true },
    { key: 'year' as keyof Vehicle, label: 'Ano', sortable: true },
    {
      key: 'status' as keyof Vehicle,
      label: 'Status',
      type: 'status',
      sortable: true,
    },
  ];

  vehicleFilters = [
    { key: 'plate', label: 'Placa', type: 'text', placeholder: 'Placa...' },
    { key: 'model', label: 'Modelo', type: 'text', placeholder: 'Modelo...' },
    { key: 'brand', label: 'Marca', type: 'text', placeholder: 'Marca...' },
    { key: 'year', label: 'Ano', type: 'number', placeholder: 'Ano...' },
    {
      key: 'status',
      label: 'Status',
      type: 'select',
      options: ['ATIVO', 'INATIVO', 'MANUTENCAO'],
    },
  ] satisfies FilterConfig[];

  vehicles: Vehicle[] = [];
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 1;
  selectedVehicle?: Vehicle;
  showModal = false;

  // filtros
  filter = {
    plate: '',
    model: '',
    brand: '',
    year: '',
    status: 'ATIVO',
  };

  // ordenação
  sortKey: keyof Vehicle = 'plate';
  sortAsc = true;

  ngOnInit() {
    this.listVehicles(1, 10);
  }

  listVehicles(page: number, limit: number) {
    this.serviceVehicle
      .getAll(page, limit, this.filter, this.sortKey, this.sortAsc)
      .subscribe({
        next: (res: PaginatedResponse<Vehicle>) => {
          this.vehicles = res.data;
          this.total = res.total;
          this.page = res.page;
          this.limit = res.limit;
          this.totalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.log('Erro ao carregar veículos:', err);
          this.vehicles = [];
          this.total = 0;
          this.totalPages = 0;
        },
      });
  }

  applyFilters() {
    this.page = 1;
    this.listVehicles(this.page, this.limit);
    this.cdr.detectChanges();
  }

  sortBy(key: keyof Vehicle) {
    if (this.sortKey === key) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortKey = key;
      this.sortAsc = true;
    }
    this.listVehicles(this.page, this.limit);
  }

  clearFilters() {
    this.filter = { plate: '', model: '', brand: '', year: '', status: '' };
    this.applyFilters();
  }

  onPageChange(newPage: number) {
    this.listVehicles(newPage, this.limit);
  }

  onEdit(vehicle: Vehicle) {
    this.selectedVehicle = { ...vehicle };
    this.showModal = true;
  }

  onCloseModal() {
    this.showModal = false;
  }

  onSaveModal(vehicle: Vehicle) {
    const id = vehicle.id;
    delete vehicle.id;

    this.serviceVehicle.update(id!, vehicle).subscribe({
      next: () => {
        this.listVehicles(1, 10);
        this.showModal = false;
        this.selectedVehicle = undefined;
        alertSuccess('Veículo atualizado com Sucesso');
      },
      error: (err) => {
        alertError(
          `Ocorreu um erro ao salvar o veículo. ${
            err?.error?.message || 'Erro desconhecido.'
          }`
        );
      },
    });
  }

  onNavDetails(id?: number) {
    if (!id) return;
    this.router.navigate(['/veiculos', id]);
  }
}
