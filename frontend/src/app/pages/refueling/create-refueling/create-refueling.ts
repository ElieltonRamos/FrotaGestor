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
import { Trip } from '../../../interfaces/trip';
import { VehicleService } from '../../../services/vehicle.service';
import { DriverService } from '../../../services/driver.service';
import { TripService } from '../../../services/trip.service';
import { SelectModalComponent } from '../../../components/select-modal.component/select-modal.component';
import { ColumnConfig } from '../../../components/base-list-component/base-list-component';
import { FilterConfig } from '../../../components/base-filter-component/base-filter-component';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-create-refueling',
  imports: [DynamicFormComponent, DecimalPipe],
  templateUrl: './create-refueling.html',
  styles: ``,
})
export class CreateRefueling {
  private expenseService = inject(ExpenseService);
  private vehicleService = inject(VehicleService);
  private driverService = inject(DriverService);
  private tripService = inject(TripService);
  pricePerLiter: number = 0;

  refuelingFields: FormField[] = [
    {
      placeholder: 'Posto de Combustível',
      name: 'description',
      label: 'Descrição',
      type: 'text',
    },
    {
      placeholder: 'Valor Total',
      name: 'amount',
      label: 'Valor Total',
      type: 'number',
      required: true,
    },
    {
      placeholder: 'Litros abastecidos',
      name: 'liters',
      label: 'Litros',
      type: 'number',
      required: true,
    },
    {
      placeholder: 'Data do Abastecimento',
      name: 'date',
      label: 'Data',
      type: 'date',
      required: true,
    },
    {
      placeholder: 'Odômetro',
      name: 'odometer',
      label: 'Odômetro',
      type: 'number',
    },
  ];

  selectedVehicle?: Vehicle;
  selectedDriver?: Driver;
  selectedTrip?: Trip;

  showVehicleModal = false;
  showDriverModal = false;
  showTripModal = false;

  // colunas e filtros (reuso do que já tinha em CreateExpense)
  vehicleColumns: ColumnConfig<Vehicle>[] = [
    { key: 'plate', label: 'Placa', sortable: true },
    { key: 'model', label: 'Modelo', sortable: true },
    { key: 'brand', label: 'Marca', sortable: true },
    { key: 'status', label: 'Status', type: 'status', sortable: true },
  ];
  vehicleFilters: FilterConfig[] = [
    { key: 'plate', label: 'Placa', type: 'text', placeholder: 'Placa...' },
    { key: 'model', label: 'Modelo', type: 'text', placeholder: 'Modelo...' },
    { key: 'brand', label: 'Marca', type: 'text', placeholder: 'Marca...' },
    { key: 'status', label: 'Status', type: 'select', options: ['ATIVO'] },
  ];

  driverColumns: ColumnConfig<Driver>[] = [
    { key: 'name', label: 'Nome', sortable: true },
    { key: 'cpf', label: 'CPF', sortable: true },
    { key: 'cnh', label: 'CNH', sortable: true },
    { key: 'cnhCategory', label: 'Categoria CNH', sortable: true },
    { key: 'status', label: 'Status', type: 'status', sortable: true },
  ];
  driverFilters: FilterConfig[] = [
    { key: 'name', label: 'Nome', type: 'text', placeholder: 'Nome...' },
    { key: 'cpf', label: 'CPF', type: 'text', placeholder: 'CPF...' },
    { key: 'cnh', label: 'CNH', type: 'text', placeholder: 'CNH...' },
    { key: 'status', label: 'Status', type: 'select', options: ['ATIVO'] },
  ];

  tripColumns: ColumnConfig<Trip>[] = [
    { key: 'id', label: 'ID', sortable: true },
    { key: 'vehiclePlate', label: 'Veículo', sortable: true },
    { key: 'driverName', label: 'Motorista', sortable: true },
    { key: 'startLocation', label: 'Origem', sortable: true },
    { key: 'endLocation', label: 'Destino', sortable: true },
    { key: 'status', label: 'Status', type: 'status', sortable: true },
  ];
  tripFilters: FilterConfig[] = [
    { key: 'id', label: 'ID', type: 'number', placeholder: 'ID...' },
    {
      key: 'vehiclePlate',
      label: 'Veículo',
      type: 'text',
      placeholder: 'Placa...',
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
      placeholder: 'Origem...',
    },
    {
      key: 'endLocation',
      label: 'Destino',
      type: 'text',
      placeholder: 'Destino...',
    },
    {
      key: 'status',
      label: 'Status',
      type: 'select',
      options: Object.values(['PLANEJADA', 'EM_ANDAMENTO', 'CONCLUIDA']),
    },
  ];

  // seleção
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

  onFormChange(formData: any) {
    if (formData.amount && formData.liters && formData.liters > 0) {
      this.pricePerLiter = formData.amount / formData.liters;
    } else {
      this.pricePerLiter = 0;
    }
  }

  // fetchers
  vehicleFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Vehicle,
    sortAsc: boolean
  ) =>
    this.vehicleService.getAll(
      page,
      limit,
      filters,
      sortKey as string,
      sortAsc
    );
  driverFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Driver,
    sortAsc: boolean
  ) =>
    this.driverService.getAll(page, limit, filters, sortKey as string, sortAsc);
  tripFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Trip,
    sortAsc: boolean
  ) =>
    this.tripService.getAll(page, limit, filters, sortKey as string, sortAsc);

  // salvar abastecimento
  saveRefueling(data: Expense) {
    const payload: Expense = {
      ...data,
      type: ExpenseType.COMBUSTIVEL, // 🔹 sempre combustível
      pricePerLiter: this.pricePerLiter,
      vehicleId: this.selectedVehicle?.id ?? null,
      driverId: this.selectedDriver?.id ?? null,
      tripId: this.selectedTrip?.id ?? null,
    };

    this.expenseService.create(payload).subscribe({
      next: () => alertSuccess(`Abastecimento registrado com sucesso`),
      error: (e) =>
        alertError(
          `Erro ao registrar abastecimento: ${
            e.error?.message || 'Erro desconhecido'
          }`
        ),
    });
  }
}
