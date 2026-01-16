import { Component, inject, OnInit } from '@angular/core';
import {
  DynamicFormComponent,
  FormField,
} from '../../../components/dynamic-form/dynamic-form';
import { VehicleService } from '../../../services/vehicle.service';
import { SubfleetService } from '../../../services/subfleet.service';
import { Vehicle } from '../../../interfaces/vehicle';
import { Subfleet } from '../../../interfaces/subfleet';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { SelectModalComponent } from '../../../components/select-modal.component/select-modal.component';
import { ColumnConfig } from '../../../components/base-list-component/base-list-component';
import { FilterConfig } from '../../../components/base-filter-component/base-filter-component';

@Component({
  selector: 'app-create-vehicle',
  imports: [DynamicFormComponent, SelectModalComponent],
  templateUrl: './create-vehicle.html',
  styles: ``,
})
export class CreateVehicle implements OnInit {
  private vehicleService = inject(VehicleService);
  private subfleetService = inject(SubfleetService);

  vehicleFields: FormField[] = [
    {
      placeholder: 'Placa',
      name: 'plate',
      label: 'Placa',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'Modelo',
      name: 'model',
      label: 'Modelo',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'Marca',
      name: 'brand',
      label: 'Marca',
      type: 'text',
    },
    {
      placeholder: 'Ano',
      name: 'year',
      label: 'Ano',
      type: 'number',
    },
    {
      placeholder: 'Status',
      name: 'status',
      label: 'Status',
      type: 'select',
      options: ['ATIVO', 'INATIVO', 'MANUTENCAO'],
      required: true,
    },
  ];

  // Subfrota
  subfleetColumns: ColumnConfig<Subfleet>[] = [
    { key: 'name', label: 'Nome', sortable: true },
    { key: 'description', label: 'Descrição', sortable: false },
    { key: 'vehicleCount', label: 'Veículos', sortable: true },
  ];

  subfleetFilters: FilterConfig[] = [
    { key: 'name', label: 'Nome', type: 'text', placeholder: 'Nome...' },
  ];

  selectedSubfleet?: Subfleet;
  showSubfleetModal = false;

  onSubfleetSelect(subfleet: Subfleet) {
    this.selectedSubfleet = subfleet;
    this.showSubfleetModal = false;
  }

  subfleetFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Subfleet,
    sortAsc: boolean
  ) => {
    return this.subfleetService.getAll(page, limit, filters);
  };

  saveVehicle(data: any) {
    if (!data.plate || !data.model) {
      alertError('Placa e modelo são obrigatórios');
      return;
    }

    const payload: Partial<Vehicle> = {
      plate: data.plate,
      model: data.model,
      brand: data.brand || null,
      year: data.year || null,
      status: data.status || 'ATIVO',
      subfleetId: this.selectedSubfleet?.id || null,  // ✅ Opcional
    };

    this.vehicleService.create(payload as Vehicle).subscribe({
      next: () => {
        alertSuccess('Veículo cadastrado com sucesso!');
      },
      error: (e) => {
        alertError(
          `Erro ao cadastrar veículo: ${
            e.error?.message || 'Erro desconhecido'
          }`
        );
      },
    });
  }

  ngOnInit() {
    // ✅ Status já tem default 'ATIVO' no FormField
  }
}
