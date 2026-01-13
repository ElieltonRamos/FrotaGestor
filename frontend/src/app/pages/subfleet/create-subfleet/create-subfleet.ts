import { Component, inject } from '@angular/core';
import { SubfleetService } from '../../../services/subfleet.service';
import { CreateSubfleetRequest, DEFAULT_SUBFLEET_VALUES, SubfleetStatus } from '../../../interfaces/subfleet';
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

  // Se o seu DynamicForm suportar "initialValues", isso já preenche cor/ícone/status.
  initialValues: Partial<CreateSubfleetRequest> = {
    color: DEFAULT_SUBFLEET_VALUES.color,
    icon: DEFAULT_SUBFLEET_VALUES.icon,
    status: DEFAULT_SUBFLEET_VALUES.status,
  };

  subfleetFields: FormField[] = [
    {
      placeholder: 'Nome',
      name: 'name',
      label: 'Nome da Subfrota',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'Descrição',
      name: 'description',
      label: 'Descrição',
      type: 'text',
    },
  ];

  saveSubfleet(data: any) {
    const payload: CreateSubfleetRequest = {
      name: data?.name,
      description: data?.description || undefined,
      parentId: data?.parentId ? Number(data.parentId) : undefined,
      color: data?.color || DEFAULT_SUBFLEET_VALUES.color,
      icon: data?.icon || DEFAULT_SUBFLEET_VALUES.icon,
      managerUserId: data?.managerUserId
        ? Number(data.managerUserId)
        : undefined,
      status:
        (data?.status as SubfleetStatus) || DEFAULT_SUBFLEET_VALUES.status,
    };

    this.subfleetService.create(payload).subscribe({
      next: () => alertSuccess('Subfrota cadastrada'),
      error: (e) =>
        alertError(
          `Erro ao cadastrar subfrota: ${
            e?.error?.message ?? 'Erro desconhecido'
          }`
        ),
    });
  }
}
