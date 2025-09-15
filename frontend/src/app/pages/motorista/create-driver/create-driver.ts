import { Component } from '@angular/core';
import { DynamicFormComponent, FormField } from '../../../components/dynamic-form/dynamic-form';

@Component({
  selector: 'app-create-driver',
  imports: [DynamicFormComponent],
  templateUrl: './create-driver.html',
  styles: ``,
})
export class CreateDriverComponent {
  motoristaFields: FormField[] = [
    {
      placeholder: 'Nome',
      name: 'nome',
      label: 'Nome Completo',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'CPF',
      name: 'cpf',
      label: 'CPF',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'CNH',
      name: 'cnh',
      label: 'CNH',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'Categoria CNH',
      name: 'categoriaCnh',
      label: 'Categoria CNH',
      type: 'text',
    },
    {
      placeholder: 'Validade CNH',
      name: 'validadeCnh',
      label: 'Validade CNH',
      type: 'date',
    },
    {
      placeholder: 'Telefone',
      name: 'telefone',
      label: 'Telefone',
      type: 'text',
    },
  ];

  saveDriver(data: any) {
    console.log('Motorista cadastrado:', data);
  }
}
