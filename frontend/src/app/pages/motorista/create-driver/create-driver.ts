import { Component, inject } from '@angular/core';
import { DynamicFormComponent, FormField } from '../../../components/dynamic-form/dynamic-form';
import { DriverService } from '../../../services/driver.service';
import { Driver } from '../../../interfaces/driver';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

@Component({
  selector: 'app-create-driver',
  imports: [DynamicFormComponent],
  templateUrl: './create-driver.html',
  styles: ``,
})
export class CreateDriverComponent {
  private driverServeice = inject(DriverService)

  motoristaFields: FormField[] = [
    {
      placeholder: 'Nome',
      name: 'name',
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
      name: 'cnhCategory',
      label: 'Categoria CNH',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'Validade CNH',
      name: 'cnhExpiration',
      label: 'Validade CNH',
      type: 'date',
      required: true,
    },
    {
      placeholder: 'Telefone',
      name: 'phone',
      label: 'Telefone',
      type: 'text',
    },
    {
      placeholder: 'E-mail',
      name: 'email',
      label: 'E-mail',
      type: 'email',
    }
  ];

  saveDriver(data: Driver) {
    console.log(data, 'data')
    this.driverServeice.create(data).subscribe({
      next: (res) => {
        alertSuccess(`Motorista Cadastrado`)
      },
      error: (e) => {
        alertError(`Erro ao cadastrar motorista: ${e.error.message}`)
      }
    })
  }
}
