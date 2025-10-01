import { Component, inject } from '@angular/core';
import {
  DynamicFormComponent,
  FormField,
} from '../../../components/dynamic-form/dynamic-form';
import { ExpenseService } from '../../../services/expense.service';
import { Expense, ExpenseType } from '../../../interfaces/expense';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { Vehicle } from '../../../interfaces/vehicle';
import { Driver } from '../../../interfaces/driver';
import { VehicleService } from '../../../services/vehicle.service';
import { DriverService } from '../../../services/driver.service';
import { TripService } from '../../../services/trip.service';
import { Trip, TripStatus } from '../../../interfaces/trip';
import { SelectModalComponent } from '../../../components/select-modal.component/select-modal.component';
import { ColumnConfig } from '../../../components/base-list-component/base-list-component';
import { FilterConfig } from '../../../components/base-filter-component/base-filter-component';

@Component({
  selector: 'app-create-expense',
  imports: [DynamicFormComponent, SelectModalComponent],
  templateUrl: './create-expense.html',
  styles: ``,
})
export class CreateExpense {
  private expenseService = inject(ExpenseService);
  private vehicleService = inject(VehicleService);
  private driverService = inject(DriverService);
  private tripService = inject(TripService);

  expenseFields: FormField[] = [
    {
      placeholder: 'Descrição',
      name: 'description',
      label: 'Descrição',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'Valor',
      name: 'amount',
      label: 'Valor',
      type: 'number',
      required: true,
    },
    {
      placeholder: 'Tipo',
      name: 'type',
      label: 'Tipo',
      type: 'select',
      options: Object.values(ExpenseType),
      required: true,
    },
    {
      placeholder: 'Data da Despesa',
      name: 'date',
      label: 'Data da Despesa',
      type: 'date',
      required: true,
    },
    {
      placeholder: 'Litros abastecidos',
      name: 'liters',
      label: 'Litros',
      type: 'number',
    },
    {
      placeholder: 'Preço por Litro',
      name: 'pricePerLiter',
      label: 'Preço por Litro',
      type: 'number',
    },
    {
      placeholder: 'Odômetro',
      name: 'odometer',
      label: 'Odômetro',
      type: 'number',
    },
  ];

  selectedVehicle: Vehicle | undefined;
  selectedDriver: Driver | undefined;
  selectedTrip: Trip | undefined;

  showVehicleModal = false;
  showDriverModal = false;
  showTripModal = false;

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

  vehicleColumns: ColumnConfig<Vehicle>[] = [
    { key: 'plate' as keyof Vehicle, label: 'Placa', sortable: true },
    { key: 'model' as keyof Vehicle, label: 'Modelo', sortable: true },
    { key: 'brand' as keyof Vehicle, label: 'Marca', sortable: true },
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
    {
      key: 'status',
      label: 'Status',
      type: 'select',
      options: ['ATIVO'],
    },
  ] satisfies FilterConfig[];

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
    {
      key: 'status',
      label: 'Status',
      type: 'select',
      options: Object.values(TripStatus),
    },
  ] satisfies FilterConfig[];

  onVehicleSelect(vehicle: Vehicle) {
    this.selectedVehicle = vehicle;
    this.showVehicleModal = false;
  }

  onDriverSelect(driver: Driver) {
    this.selectedDriver = driver;
    this.showDriverModal = false;
  }

  onTripSelect(trip: Trip) {
    this.selectedTrip = trip;
    this.showTripModal = false;
  }

  vehicleFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Vehicle,
    sortAsc: boolean
  ) => {
    return this.vehicleService.getAll(
      page,
      limit,
      filters,
      sortKey as string,
      sortAsc
    );
  };

  driverFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Driver,
    sortAsc: boolean
  ) => {
    return this.driverService.getAll(
      page,
      limit,
      filters,
      sortKey as string,
      sortAsc
    );
  };

  tripFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Trip,
    sortAsc: boolean
  ) => {
    return this.tripService.getAll(
      page,
      limit,
      filters,
      sortKey as string,
      sortAsc
    );
  };

  saveExpense(data: Expense) {
    const payload: Expense = {
      ...data,
      vehicleId: this.selectedVehicle?.id ?? null,
      driverId: this.selectedDriver?.id ?? null,
      tripId: this.selectedTrip?.id ?? null,
    };

    this.expenseService.create(payload).subscribe({
      next: () => {
        alertSuccess(`Despesa cadastrada com sucesso`);
      },
      error: (e) => {
        alertError(
          `Erro ao cadastrar despesa: ${
            e.error?.message || 'Erro desconhecido'
          }`
        );
      },
    });
  }
}
