import { Component, inject } from '@angular/core';
import { SubfleetService } from '../../../services/subfleet.service';
import { FormField, DynamicFormComponent } from '../../../components/dynamic-form/dynamic-form';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

@Component({
  selector: 'app-create-subfleet',
  imports: [DynamicFormComponent],
  templateUrl: './create-subfleet.html',
  styles: ``,
})
export class CreateSubfleet {
  private subfleetService = inject(SubfleetService);

  subfleetFields: FormField[] = [
    {
      placeholder: 'Nome',
      name: 'name',
      label: 'Nome da Subfrota',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'Descrição (opcional)',
      name: 'description',
      label: 'Descrição',
      type: 'text',
    },
  ];

  saveSubfleet(data: any) {
    const payload = {
      name: data?.name,
      description: data?.description || undefined,
    };

    this.subfleetService.create(payload).subscribe({
      next: () => {
        alertSuccess('Subfrota cadastrada com sucesso!');
      },
      error: (e) => {
        alertError(
          `Erro ao cadastrar subfrota: ${
            e?.error?.message ?? 'Erro desconhecido'
          }`
        );
      },
    });
  }
}
