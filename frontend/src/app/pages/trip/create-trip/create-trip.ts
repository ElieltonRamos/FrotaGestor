import { Component, inject } from '@angular/core';
import { DynamicFormComponent } from '../../../components/dynamic-form/dynamic-form';
import { TripService } from '../../../services/trip.service';
import { Trip } from '../../../interfaces/trip';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

@Component({
  selector: 'app-create-trip',
  imports: [DynamicFormComponent],
  templateUrl: './create-trip.html',
  styles: ``,
})
export class CreateTrip {
  private tripService = inject(TripService);
  tripFields = [
    {
      name: 'vehicleId',
      label: 'ID do Veículo',
      type: 'number',
      placeholder: 'Digite o ID do veículo',
    },
    {
      name: 'driverId',
      label: 'ID do Motorista',
      type: 'number',
      placeholder: 'Digite o ID do motorista',
    },
    {
      name: 'startLocation',
      label: 'Origem',
      type: 'text',
      placeholder: 'Digite o local de origem',
    },
    {
      name: 'endLocation',
      label: 'Destino',
      type: 'text',
      placeholder: 'Digite o local de destino',
    },
    {
      name: 'startTime',
      label: 'Data/Hora Início',
      type: 'datetime-local',
    },
    {
      name: 'endTime',
      label: 'Data/Hora Fim',
      type: 'datetime-local',
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
    },
  ];

  saveTrip(data: Trip) {
    console.log(data, 'data');
    this.tripService.create(data).subscribe({
      next: (res) => {
        alertSuccess(`Viagen Criada`);
      },
      error: (e) => {
        alertError(`Erro ao criar viagen: ${e.error.message}`);
      },
    });
  }
}
