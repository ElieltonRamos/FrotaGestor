import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ColumnConfig,
  BaseListComponent,
} from '../../../components/base-list-component/base-list-component';
import {
  Expense,
  ExpenseType,
  MaintenanceIndicators,
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
  selector: 'app-list-maintenance',
  imports: [
    BaseFilterComponent,
    PaginatorComponent,
    BaseListComponent,
    ModalEditComponent,
    CurrencyPipe,
    DatePipe,
  ],
  templateUrl: './list-maintenance.html',
  styles: ``,
})
export class ListMaintenance {
  private serviceExpense = inject(ExpenseService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  loadingIndicators = false;
  indicators?: MaintenanceIndicators;

  // 🔹 Campos do formulário de manutenção
  maintenanceFields = [
    { name: 'description', label: 'Descrição', type: 'text' },
    { name: 'amount', label: 'Valor', type: 'number' },
    { name: 'type', label: 'Tipo', type: 'text' },
    { name: 'date', label: 'Data da Manutenção', type: 'date' },
  ];

  // 🔹 Colunas da tabela
  maintenanceColumns: ColumnConfig<Expense>[] = [
    { key: 'description', label: 'Descrição', sortable: true },
    { key: 'amount', label: 'Valor', sortable: true },
    { key: 'type', label: 'Tipo', sortable: true },
    { key: 'vehiclePlate', label: 'Placa', sortable: true },
    { key: 'date', label: 'Data', type: 'date', sortable: true },
  ];

  // 🔹 Filtros
  maintenanceFilters: FilterConfig[] = [
    {
      key: 'description',
      label: 'Descrição',
      type: 'text',
      placeholder: 'Descrição...',
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

  maintenances: Expense[] = [];
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 1;
  selectedMaintenance?: Expense;
  showModal = false;

  filter: Partial<Expense & { startDate?: string; endDate?: string }> = {
    type: ExpenseType.MANUTENCAO,
  };

  sortKey: keyof Expense = 'date';
  sortAsc = true;

  ngOnInit() {
    this.listMaintenances(1, 10);
  }

  listMaintenances(page: number, limit: number) {
    this.serviceExpense
      .getAll(page, limit, this.filter, this.sortKey, this.sortAsc)
      .subscribe({
        next: (res: PaginatedResponse<Expense>) => {
          this.maintenances = res.data;
          this.total = res.total;
          this.page = res.page;
          this.limit = res.limit;
          this.totalPages = res.totalPages;
          this.loadIndicators();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.maintenances = [];
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

    this.serviceExpense.getIndicatorsMaintenance(filterWithPeriod).subscribe({
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
          topVehicleByAmount: { plate: '', amount: 0 },
          lastMaintenance: { date: '', plate: '' },
        };
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
    });
  }

  applyFilters() {
    this.page = 1;
    this.listMaintenances(this.page, this.limit);
    this.loadIndicators();
  }

  sortBy(key: keyof Expense) {
    if (this.sortKey === key) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortKey = key;
      this.sortAsc = true;
    }
    this.listMaintenances(this.page, this.limit);
  }

  clearFilters() {
    this.filter = { type: ExpenseType.MANUTENCAO };
    this.applyFilters();
  }

  onPageChange(newPage: number) {
    this.listMaintenances(newPage, this.limit);
  }

  onEdit(maintenance: Expense) {
    this.selectedMaintenance = { ...maintenance };
    this.showModal = true;
  }

  onCloseModal() {
    this.showModal = false;
  }

  onSaveModal(maintenance: Expense) {
    const id = maintenance.id || 0;
    delete maintenance.id;
    delete maintenance.vehiclePlate;
    delete maintenance.driverName;
    maintenance.type = ExpenseType.MANUTENCAO;

    this.serviceExpense.update(id, maintenance).subscribe({
      next: () => {
        this.listMaintenances(1, 10);
        this.loadIndicators();
        this.showModal = false;
        this.selectedMaintenance = undefined;
        alertSuccess('Manutenção atualizada com sucesso');
      },
      error: (err) => {
        alertError(
          `Erro ao salvar Manutenção: ${
            err?.error?.message || 'Erro desconhecido.'
          }`
        );
      },
    });
  }

  onNavDetails(id?: Number) {
    if (!id) return;
    this.router.navigate(['/manutencoes', id]);
  }
}
