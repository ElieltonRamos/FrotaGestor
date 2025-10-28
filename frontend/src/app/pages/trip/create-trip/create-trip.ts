import { Component, inject } from '@angular/core';
import { DynamicFormComponent } from '../../../components/dynamic-form/dynamic-form';
import { TripService } from '../../../services/trip.service';
import { Trip } from '../../../interfaces/trip';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { Vehicle } from '../../../interfaces/vehicle';
import { Driver } from '../../../interfaces/driver';
import { SelectModalComponent } from '../../../components/select-modal.component/select-modal.component';
import { VehicleService } from '../../../services/vehicle.service';
import { DriverService } from '../../../services/driver.service';
import { ColumnConfig } from '../../../components/base-list-component/base-list-component';
import { FilterConfig } from '../../../components/base-filter-component/base-filter-component';

@Component({
  selector: 'app-create-trip',
  imports: [DynamicFormComponent, SelectModalComponent],
  templateUrl: './create-trip.html',
  styles: ``,
})
export class CreateTrip {
  private tripService = inject(TripService);
  private vehicleService = inject(VehicleService);
  private driverService = inject(DriverService);

  tripFields = [
    {
      name: 'startLocation',
      label: 'Origem',
      type: 'text',
      placeholder: 'Digite o local de origem',
      required: true,
    },
    {
      name: 'endLocation',
      label: 'Destino',
      type: 'text',
      placeholder: 'Digite o local de destino',
      required: true,
    },
    {
      name: 'startTime',
      label: 'Data/Hora Início',
      type: 'datetime-local',
      placeholder: 'Selecione a data e hora de início',
      required: true,
    },
    {
      name: 'endTime',
      label: 'Data/Hora Fim',
      type: 'datetime-local',
      placeholder: 'Selecione a data e hora de término',
    },
    {
      name: 'distanceKm',
      label: 'Distância (Km)',
      type: 'number',
      placeholder: 'Digite a distância em Km',
    },
    {
      name: 'status',
      label: 'Status',
      type: 'select',
      options: ['PLANEJADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA'],
      required: true,
      placeholder: 'Selecione o status',
    },
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

  selectedVehicle?: Vehicle;
  selectedDriver?: Driver;

  showVehicleModal = false;
  showDriverModal = false;

  onVehicleSelect(vehicle: Vehicle) {
    this.selectedVehicle = vehicle;
    this.showVehicleModal = false;
  }

  onDriverSelect(driver: Driver) {
    this.selectedDriver = driver;
    this.showDriverModal = false;
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

  saveTrip(data: Trip) {
    if (!this.selectedVehicle || !this.selectedDriver) {
      alertError('Selecione veículo e motorista antes de salvar');
      return;
    }

    const payload: Trip = {
      ...data,
      vehicleId: this.selectedVehicle.id!,
      driverId: this.selectedDriver.id!,
    };

    this.tripService.create(payload).subscribe({
      next: () => alertSuccess(`Viagem criada`),
      error: (e) => alertError(`Erro ao criar viagem: ${e.error.message}`),
    });
  }
}
