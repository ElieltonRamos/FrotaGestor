import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Vehicle, VehicleIndicators } from '../../../interfaces/vehicle';
import { Subfleet } from '../../../interfaces/subfleet';
import { VehicleService } from '../../../services/vehicle.service';
import { SubfleetService } from '../../../services/subfleet.service';
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
import { SelectModalComponent } from '../../../components/select-modal.component/select-modal.component';
import { Driver } from '../../../interfaces/driver';
import { DriverService } from '../../../services/driver.service';

@Component({
  selector: 'app-list-vehicle',
  imports: [
    FormsModule,
    CommonModule,
    BaseListComponent,
    PaginatorComponent,
    BaseFilterComponent,
    ModalEditComponent,
    SelectModalComponent,
  ],
  templateUrl: './list-vehicle.html',
})
export class ListVehicle {
  private serviceVehicle = inject(VehicleService);
  private subfleetService = inject(SubfleetService);
  private driverService = inject(DriverService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  indicators?: VehicleIndicators;
  loadingIndicators = false;
  showDriverModal = false;
  selectedDriver?: Driver;

  vehicleFields = [
    { name: 'plate', label: 'Placa', type: 'text' },
    { name: 'model', label: 'Modelo', type: 'text' },
    { name: 'brand', label: 'Marca', type: 'text' },
    { name: 'modelYear', label: 'Ano Modelo', type: 'number' },
    { name: 'manufacturingYear', label: 'Ano Fabricação', type: 'number' },
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
    { key: 'subfleetName' as keyof Vehicle, label: 'Subfrota', sortable: true }, // ✅
    { key: 'year' as keyof Vehicle, label: 'Ano', sortable: true },
    {
      key: 'status' as keyof Vehicle,
      label: 'Status',
      type: 'status',
      sortable: true,
    },
  ];

  vehicleFilters: FilterConfig[] = [
    { key: 'plate', label: 'Placa', type: 'text', placeholder: 'Placa...' },
    { key: 'model', label: 'Modelo', type: 'text', placeholder: 'Modelo...' },
    { key: 'brand', label: 'Marca', type: 'text', placeholder: 'Marca...' },
    {
      key: 'subfleetName',
      label: 'Subfrota',
      type: 'text',
      placeholder: 'Nome da subfrota...',
    }, // ✅
    { key: 'year', label: 'Ano', type: 'number', placeholder: 'Ano...' },
    {
      key: 'status',
      label: 'Status',
      type: 'select',
      options: ['ATIVO', 'INATIVO', 'MANUTENCAO'],
    },
  ];

  // Subfrota Modal
  subfleetColumns: ColumnConfig<Subfleet>[] = [
    { key: 'name', label: 'Nome', sortable: true },
    { key: 'description', label: 'Descrição', sortable: false },
    { key: 'vehicleCount', label: 'Veículos', sortable: true },
  ];

  subfleetFilters: FilterConfig[] = [
    { key: 'name', label: 'Nome', type: 'text', placeholder: 'Nome...' },
  ];

  driverColumns: ColumnConfig<Driver>[] = [
    { key: 'name', label: 'Nome', sortable: true },
    { key: 'cpf', label: 'CPF', sortable: true },
    { key: 'cnh', label: 'CNH', sortable: true },
    { key: 'cnhCategory', label: 'Categoria CNH', sortable: true },
    {
      key: 'cnhExpiration',
      label: 'Validade CNH',
      type: 'date',
      sortable: true,
    },
    { key: 'status', label: 'Status', type: 'status', sortable: true },
  ];

  driverFilters: FilterConfig[] = [
    { key: 'name', label: 'Nome', type: 'text', placeholder: 'Nome...' },
    { key: 'cpf', label: 'CPF', type: 'text', placeholder: 'CPF...' },
    { key: 'cnh', label: 'CNH', type: 'text', placeholder: 'CNH...' },
    {
      key: 'cnhCategory',
      label: 'Categoria CNH',
      type: 'text',
      placeholder: 'Categoria CNH...',
    },
    {
      key: 'cnhExpiration',
      label: 'Validade CNH',
      type: 'text',
      placeholder: 'Validade CNH...',
    },
    {
      key: 'status',
      label: 'Status',
      type: 'select',
      options: ['ATIVO'],
    },
  ];

  vehicles: Vehicle[] = [];
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 1;
  selectedVehicle?: Vehicle;
  showModal = false;
  showSubfleetModal = false;
  selectedSubfleet?: Subfleet;

  // filtros
  filter: any = {
    plate: '',
    model: '',
    brand: '',
    year: '',
    subfleetName: '', // ✅
    status: 'ATIVO',
  };

  // ordenação
  sortKey: keyof Vehicle = 'plate';
  sortAsc = true;

  ngOnInit() {
    this.listVehicles(1, 10);
    this.loadIndicators();
  }

  listVehicles(page: number, limit: number) {
    this.serviceVehicle
      .getAll(page, limit, this.filter, this.sortKey as string, this.sortAsc)
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
          console.error('Erro ao carregar veículos:', err);
          this.vehicles = [];
          this.total = 0;
          this.totalPages = 0;
        },
      });
  }

  loadIndicators() {
    this.loadingIndicators = true;
    this.serviceVehicle.getIndicators({}).subscribe({
      next: (res: VehicleIndicators) => {
        this.indicators = res;
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao carregar indicadores:', err);
        this.loadingIndicators = false;
      },
    });
  }

  driverFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Driver,
    sortAsc: boolean,
  ) => {
    return this.driverService.getAll(
      page,
      limit,
      filters,
      sortKey as string,
      sortAsc,
    );
  };

  onDriverSelect(driver: Driver) {
    this.selectedDriver = driver;
    this.showDriverModal = false;
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
    this.filter = {
      plate: '',
      model: '',
      brand: '',
      year: '',
      subfleetName: '',
      status: 'ATIVO',
    };
    this.applyFilters();
  }

  onPageChange(newPage: number) {
    this.listVehicles(newPage, this.limit);
  }

  onEdit(vehicle: Vehicle) {
    this.selectedVehicle = { ...vehicle };
    this.selectedSubfleet = vehicle.subfleetId
      ? ({
          id: vehicle.subfleetId,
          name: vehicle.subfleetName || '',
        } as Subfleet)
      : undefined;
    this.showModal = true;
  }

  onCloseModal() {
    this.showModal = false;
    this.selectedSubfleet = undefined;
  }

  // Subfrota Modal
  subfleetFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Subfleet,
    sortAsc: boolean,
  ) => {
    return this.subfleetService.getAll(page, limit, filters);
  };

  onSubfleetSelect(subfleet: Subfleet) {
    this.selectedSubfleet = subfleet;
    if (this.selectedVehicle) {
      this.selectedVehicle.subfleetId = subfleet.id;
      this.selectedVehicle.subfleetName = subfleet.name;
    }
    this.showSubfleetModal = false;
  }

  onSaveModal(vehicle: Vehicle) {
    const id = vehicle.id!;

    const updateData: Vehicle = {
      plate: vehicle.plate,
      model: vehicle.model,
      brand: vehicle.brand || null,
      modelYear: vehicle.modelYear || null,
      manufacturingYear: vehicle.manufacturingYear,
      status: vehicle.status,
      subfleetId: this.selectedSubfleet?.id || null, // ✅
      defaultDriverId: this.selectedDriver?.id
    };

    this.serviceVehicle.update(id, updateData).subscribe({
      next: () => {
        this.listVehicles(this.page, this.limit);
        this.loadIndicators();
        this.showModal = false;
        this.selectedVehicle = undefined;
        this.selectedSubfleet = undefined;
        alertSuccess('Veículo atualizado com Sucesso');
      },
      error: (err) => {
        alertError(
          `Ocorreu um erro ao salvar o veículo. ${
            err?.error?.message || 'Erro desconhecido.'
          }`,
        );
      },
    });
  }

  onNavDetails(id?: number) {
    if (!id) return;
    this.router.navigate(['/veiculos', id]);
  }
}
