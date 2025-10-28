import { Component, inject } from '@angular/core';
import {
  DynamicFormComponent,
  FormField,
} from '../../../components/dynamic-form/dynamic-form';
import { VehicleService } from '../../../services/vehicle.service';
import { Vehicle } from '../../../interfaces/vehicle';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

@Component({
  selector: 'app-create-vehicle',
  imports: [DynamicFormComponent],
  templateUrl: './create-vehicle.html',
  styles: ``,
})
export class CreateVehicle {
  private vehicleService = inject(VehicleService);

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
    }
  ];

  saveVehicle(data: Vehicle) {
    this.vehicleService.create(data).subscribe({
      next: () => {
        alertSuccess(`Veículo cadastrado com sucesso`);
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
}
