import { Component, inject } from '@angular/core';
import {
  DynamicFormComponent,
  FormField,
} from '../../../components/dynamic-form/dynamic-form';
import { ExpenseService } from '../../../services/expense.service';
import { Expense } from '../../../interfaces/expense';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

@Component({
  selector: 'app-create-expense',
  imports: [DynamicFormComponent],
  templateUrl: './create-expense.html',
  styles: ``,
})
export class CreateExpense {
  private expenseService = inject(ExpenseService);

  expenseFields: FormField[] = [
    {
      placeholder: 'Descrição',
      name: 'description',
      label: 'Descrição',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'Valor',
      name: 'amount',
      label: 'Valor',
      type: 'number',
      required: true,
    },
    {
      placeholder: 'Tipo',
      name: 'type',
      label: 'Tipo',
      type: 'text',
      required: true,
    },
    {
      placeholder: 'Data da Despesa',
      name: 'date',
      label: 'Data da Despesa',
      type: 'date',
      required: true,
    },
    {
      placeholder: 'ID do Veículo',
      name: 'vehicleId',
      label: 'Veículo',
      type: 'number',
    },
    {
      placeholder: 'ID do Motorista',
      name: 'driverId',
      label: 'Motorista',
      type: 'number',
    },
    {
      placeholder: 'ID da Viagem',
      name: 'tripId',
      label: 'Viagem',
      type: 'number',
    },
    {
      placeholder: 'Litros abastecidos',
      name: 'liters',
      label: 'Litros',
      type: 'number',
    },
    {
      placeholder: 'Preço por Litro',
      name: 'pricePerLiter',
      label: 'Preço por Litro',
      type: 'number',
    },
    {
      placeholder: 'Odômetro',
      name: 'odometer',
      label: 'Odômetro',
      type: 'number',
    },
  ];

  saveExpense(data: Expense) {
    this.expenseService.create(data).subscribe({
      next: () => {
        alertSuccess(`Despesa cadastrada com sucesso`);
      },
      error: (e) => {
        alertError(
          `Erro ao cadastrar despesa: ${
            e.error?.message || 'Erro desconhecido'
          }`
        );
      },
    });
  }
}
