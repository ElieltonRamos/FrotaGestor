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
import { SelectModalComponent } from '../../../components/select-modal.component/select-modal.component';
import { ColumnConfig } from '../../../components/base-list-component/base-list-component';
import { FilterConfig } from '../../../components/base-filter-component/base-filter-component';

@Component({
  selector: 'app-create-maintenance',
  imports: [DynamicFormComponent, SelectModalComponent],
  templateUrl: './create-maintenance.html',
  styles: ``,
})
export class CreateMaintenance {
  private expenseService = inject(ExpenseService);
  private vehicleService = inject(VehicleService);
  private driverService = inject(DriverService);

  maintenanceFields: FormField[] = [
    {
      placeholder: 'Descrição do Serviço',
      name: 'description',
      label: 'Descrição',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'Valor Total',
      name: 'amount',
      label: 'Valor Total',
      type: 'number',
      required: true,
    },
    {
      placeholder: 'Data da Manutenção',
      name: 'date',
      label: 'Data',
      type: 'date',
      required: true,
    }
  ];

  selectedVehicle?: Vehicle;
  selectedDriver?: Driver;

  showVehicleModal = false;
  showDriverModal = false;

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

  // seleção
  onVehicleSelect(vehicle: Vehicle) {
    this.selectedVehicle = vehicle;
    this.showVehicleModal = false;
  }
  onDriverSelect(driver: Driver) {
    this.selectedDriver = driver;
    this.showDriverModal = false;
  }

  // fetchers
  vehicleFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Vehicle,
    sortAsc: boolean
  ) => this.vehicleService.getAll(page, limit, filters, sortKey as string, sortAsc);

  driverFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Driver,
    sortAsc: boolean
  ) => this.driverService.getAll(page, limit, filters, sortKey as string, sortAsc);

  saveMaintenance(data: Expense) {
    if (!this.selectedVehicle) return;
    const payload: Expense = {
      ...data,
      type: ExpenseType.MANUTENCAO, // 🔹 sempre manutenção
      vehicleId: this.selectedVehicle?.id ?? null,
      driverId: this.selectedDriver?.id ?? null,
    };

    this.expenseService.create(payload).subscribe({
      next: () => alertSuccess(`Manutenção registrada com sucesso`),
      error: (e) =>
        alertError(
          `Erro ao registrar manutenção: ${e.error?.message || 'Erro desconhecido'}`
        ),
    });
  }
}
