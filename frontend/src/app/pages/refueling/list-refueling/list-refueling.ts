import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';

import {
  ColumnConfig,
  BaseListComponent,
} from '../../../components/base-list-component/base-list-component';
import {
  Expense,
  ExpenseType,
  RefuelingIndicators,
} from '../../../interfaces/expense';
import { ExpenseService } from '../../../services/expense.service';
import {
  FilterConfig,
  BaseFilterComponent,
} from '../../../components/base-filter-component/base-filter-component';
import { PaginatedResponse } from '../../../interfaces/paginator';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';
import { ModalEditComponent } from '../../../components/modal-edit-component/modal-edit-component';

@Component({
  selector: 'app-list-refueling',
  imports: [
    BaseFilterComponent,
    PaginatorComponent,
    BaseListComponent,
    ModalEditComponent,
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
  ],
  templateUrl: './list-refueling.html',
  styles: ``,
})
export class ListRefueling {
  private serviceExpense = inject(ExpenseService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  loadingIndicators = false;
  indicators?: RefuelingIndicators;

  expenseFields = [
    { name: 'description', label: 'Descrição', type: 'text' },
    { name: 'amount', label: 'Valor Total', type: 'number' },
    { name: 'liters', label: 'Litros', type: 'number' },
    { name: 'pricePerLiter', label: 'Preço/Litro', type: 'number' },
    { name: 'date', label: 'Data do Abastecimento', type: 'date' },
  ];

  expenseColumns: ColumnConfig<Expense>[] = [
    { key: 'description', label: 'Descrição', sortable: true },
    { key: 'amount', label: 'Valor Total', sortable: true },
    { key: 'liters', label: 'Litros', sortable: true },
    { key: 'pricePerLiter', label: 'Preço/Litro', sortable: true },
    { key: 'driverName', label: 'Motorista', sortable: true },
    { key: 'vehiclePlate', label: 'Placa', sortable: true },
    { key: 'date', label: 'Data', type: 'date', sortable: true },
  ];

  expenseFilters: FilterConfig[] = [
    {
      key: 'description',
      label: 'Descrição',
      type: 'text',
      placeholder: 'Descrição...',
    },
    {
      key: 'driverName',
      label: 'Motorista',
      type: 'text',
      placeholder: 'Motorista...',
    },
    {
      key: 'vehiclePlate',
      label: 'Placa',
      type: 'text',
      placeholder: 'Placa...',
    },
    { key: 'startDate', label: 'Data Inicial', type: 'date' },
    { key: 'endDate', label: 'Data Final', type: 'date' },
  ];

  expenses: Expense[] = [];
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 1;
  selectedExpense?: Expense;
  showModal = false;

  filter: Partial<Expense & { startDate?: string; endDate?: string }> = {
    type: ExpenseType.COMBUSTIVEL,
  };

  sortKey: keyof Expense = 'date';
  sortAsc = true;

  ngOnInit() {
    this.listexpenses(1, 10);
  }

  listexpenses(page: number, limit: number) {
    this.serviceExpense
      .getAll(page, limit, this.filter, this.sortKey, this.sortAsc)
      .subscribe({
        next: (res: PaginatedResponse<Expense>) => {
          this.expenses = res.data;
          this.total = res.total;
          this.page = res.page;
          this.limit = res.limit;
          this.totalPages = res.totalPages;
          this.loadIndicators();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.expenses = [];
          this.total = 0;
          this.totalPages = 0;
        },
      });
  }

  loadIndicators() {
    this.loadingIndicators = true;
    let filterWithPeriod = { ...this.filter };

    if (!this.filter.startDate && !this.filter.endDate) {
      const now = new Date();
      const start = new Date(now.getFullYear(), now.getMonth(), 1);
      const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);

      filterWithPeriod = {
        ...this.filter,
        startDate: start.toISOString().split('T')[0],
        endDate: end.toISOString().split('T')[0],
      };
    }

    this.serviceExpense.getIndicatorsRefueling(filterWithPeriod).subscribe({
      next: (data) => {
        this.indicators = data;
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.indicators = {
          totalAmount: 0,
          totalLiters: 0,
          avgPricePerLiter: 0,
          topDriver: { name: '', count: 0 },
          topVehicleByAmount: { plate: '', amount: 0 },
          topVehicleByLiters: { plate: '', liters: 0 },
          lastRefueling: { date: '', plate: '' },
        };
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
    });
  }

  applyFilters() {
    this.page = 1;
    this.listexpenses(this.page, this.limit);
    this.loadIndicators();
  }

  sortBy(key: keyof Expense) {
    if (this.sortKey === key) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortKey = key;
      this.sortAsc = true;
    }
    this.listexpenses(this.page, this.limit);
  }

  clearFilters() {
    this.filter = { type: ExpenseType.COMBUSTIVEL };
    this.applyFilters();
  }

  onPageChange(newPage: number) {
    this.listexpenses(newPage, this.limit);
  }

  onEdit(expense: Expense) {
    this.selectedExpense = { ...expense };
    this.showModal = true;
  }

  onCloseModal() {
    this.showModal = false;
  }

  onSaveModal(expense: Expense) {
    const id = expense.id || 0;
    delete expense.id;
    delete expense.driverName;
    delete expense.vehiclePlate;
    expense.type = ExpenseType.COMBUSTIVEL;

    this.serviceExpense.update(id, expense).subscribe({
      next: () => {
        this.listexpenses(1, 10);
        this.loadIndicators();
        this.showModal = false;
        this.selectedExpense = undefined;
        alertSuccess('Abastecimento atualizado com sucesso');
      },
      error: (err) => {
        alertError(
          `Ocorreu um erro ao salvar o Abastecimento. ${
            err?.error?.message || 'Erro desconhecido.'
          }`
        );
      },
    });
  }

  onNavDetails(id?: Number) {
    if (!id) return;
    this.router.navigate(['/abastecimentos', id]);
  }
}
