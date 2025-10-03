import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import {
  ColumnConfig,
  BaseListComponent,
} from '../../../components/base-list-component/base-list-component';
import {
  Expense,
  ExpenseIndicators,
  ExpenseType,
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
import { CurrencyPipe, DatePipe } from '@angular/common';

@Component({
  selector: 'app-list-expense',
  imports: [
    BaseFilterComponent,
    PaginatorComponent,
    BaseListComponent,
    ModalEditComponent,
    DatePipe,
    CurrencyPipe,
  ],
  templateUrl: './list-expense.html',
  styles: ``,
})
export class ListExpense {
  private serviceExpense = inject(ExpenseService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  expenseFields = [
    { name: 'description', label: 'Descrição', type: 'text' },
    { name: 'amount', label: 'Valor', type: 'number' },
    {
      name: 'type',
      label: 'Tipo',
      type: 'select',
      options: Object.values(ExpenseType),
    },
    { name: 'date', label: 'Data da Despesa', type: 'date' },
  ];

  expenseColumns: ColumnConfig<Expense>[] = [
    { key: 'description', label: 'Descrição', sortable: true },
    { key: 'amount', label: 'Valor', sortable: true },
    { key: 'type', label: 'Tipo', sortable: true },
    { key: 'driverName', label: 'Motorista', sortable: true },
    { key: 'vehiclePlate', label: 'Placa', sortable: true },
    { key: 'date', label: 'Data da Despesa', type: 'date', sortable: true },
  ];

  expenseFilters: FilterConfig[] = [
    {
      key: 'description',
      label: 'Descrição',
      type: 'text',
      placeholder: 'Descrição...',
    },
    {
      key: 'type',
      label: 'Tipo',
      type: 'select',
      placeholder: 'Tipo...',
      options: Object.values(ExpenseType),
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
    { key: 'dateStart', label: 'Data Inicial', type: 'date' },
    { key: 'dateEnd', label: 'Data Final', type: 'date' },
  ];

  expenses: Expense[] = [];
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 1;
  selectedExpense?: Expense;
  showModal = false;

  filter: Partial<Expense & { dateStart?: string; dateEnd?: string }> = {
    description: '',
    type: undefined,
    dateStart: '',
    dateEnd: '',
  };

  // ordenação
  sortKey: keyof Expense = 'description';
  sortAsc = true;
  indicators?: ExpenseIndicators;
  loadingIndicators = false;

  ngOnInit() {
    this.listexpenses(1, 10);
    this.loadIndicators();
  }

  loadIndicators() {
    this.loadingIndicators = true;
    let filterWithPeriod = { ...this.filter };

    if (!this.filter.dateStart && !this.filter.dateEnd) {
      const now = new Date();
      const start = new Date(now.getFullYear(), now.getMonth(), 1);
      const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);

      filterWithPeriod = {
        ...this.filter,
        dateStart: start.toISOString().split('T')[0],
        dateEnd: end.toISOString().split('T')[0],
      };
    }

    this.serviceExpense.getIndicatorsExpenses(filterWithPeriod).subscribe({
      next: (data) => {
        this.indicators = data;
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.indicators = {
          totalAmount: 0,
          totalCount: 0,
          mostCommonType: '',
          lastExpense: { date: '', type: '', description: '' },
        };
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
    });
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
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.log('Erro ao carregar Despesas:', err.error.message);
          this.expenses = [];
          this.total = 0;
          this.totalPages = 0;
        },
      });
  }

  applyFilters() {
    this.page = 1;
    this.listexpenses(this.page, this.limit);
    this.cdr.detectChanges();
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
    this.filter = {
      description: '',
      type: undefined,
      dateStart: '',
      dateEnd: '',
    };
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

    this.serviceExpense.update(id, expense).subscribe({
      next: () => {
        this.listexpenses(1, 10);
        this.loadIndicators();
        this.showModal = false;
        this.selectedExpense = undefined;
        alertSuccess('Despesa atualizada com sucesso');
      },
      error: (err) => {
        alertError(
          `Ocorreu um erro ao salvar a Despesa. ${
            err?.error?.message || 'Erro desconhecido.'
          }`
        );
      },
    });
  }

  onNavDetails(id?: Number) {
    if (!id) return;
    this.router.navigate(['/despesas', id]);
  }
}
