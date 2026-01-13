import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  Subfleet,
  SubfleetIndicators,
} from '../../../interfaces/subfleet';
import { SubfleetService } from '../../../services/subfleet.service';
import { PaginatedResponse } from '../../../interfaces/paginator';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';
import { ModalEditComponent } from '../../../components/modal-edit-component/modal-edit-component';
import { Router } from '@angular/router';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { CommonModule } from '@angular/common';
import {
  BaseListComponent,
  ColumnConfig,
} from '../../../components/base-list-component/base-list-component';
import {
  BaseFilterComponent,
  FilterConfig,
} from '../../../components/base-filter-component/base-filter-component';

@Component({
  selector: 'app-list-subfleet',
  imports: [
    FormsModule,
    PaginatorComponent,
    ModalEditComponent,
    CommonModule,
    BaseListComponent,
    BaseFilterComponent,
  ],
  templateUrl: './list-subfleet.html',
})
export class ListSubfleet {
  private serviceSubfleet = inject(SubfleetService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  // === Indicadores ===
  indicators?: SubfleetIndicators;
  loadingIndicators = false;

  subfleetFields = [
    { name: 'name', label: 'Nome', type: 'text' },
    { name: 'description', label: 'Descrição', type: 'text' },
  ];

  subfleetColumns: ColumnConfig<Subfleet>[] = [
    { key: 'name', label: 'Nome', sortable: true },
    { key: 'description', label: 'Descrição', sortable: false },
    { key: 'vehicleCount', label: 'Total Veículos', sortable: true },
    { key: 'activeVehicleCount', label: 'Veículos Ativos', sortable: true },
  ];

  subfleetFilters: FilterConfig[] = [
    { key: 'name', label: 'Nome', type: 'text', placeholder: 'Nome...' },
    {
      key: 'status',
      label: 'Status',
      type: 'select',
      options: ['ACTIVE', 'INACTIVE'],
    },
  ];

  subfleets: Subfleet[] = [];
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 1;
  selectedSubfleet?: Subfleet;
  showModal = false;

  // filtros
  filter: {
    name?: string;
    status?: string;
    parentId?: number;
    managerUserId?: number;
  } = {
    status: 'ACTIVE',
  };

  // ordenação
  sortKey: keyof Subfleet = 'name';
  sortAsc = true;

  ngOnInit() {
    this.listSubfleets(1, 10);
    this.loadIndicators();
  }

  listSubfleets(page: number, limit: number) {
    this.serviceSubfleet.getAll(page, limit, this.filter).subscribe({
      next: (res: PaginatedResponse<Subfleet>) => {
        this.subfleets = res.data;
        this.total = res.total;
        this.page = res.page;
        this.limit = res.limit;
        this.totalPages = res.totalPages;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao carregar subfrotas:', err);
        this.subfleets = [];
        this.total = 0;
        this.totalPages = 0;
      },
    });
  }

  loadIndicators() {
    this.loadingIndicators = true;
    // TODO: Implementar endpoint de indicadores no backend
  }

  applyFilters() {
    this.page = 1;
    this.listSubfleets(this.page, this.limit);
    this.cdr.detectChanges();
  }

  sortBy(key: keyof Subfleet) {
    if (this.sortKey === key) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortKey = key;
      this.sortAsc = true;
    }
    this.listSubfleets(this.page, this.limit);
  }

  clearFilters() {
    this.filter = {
      status: 'ACTIVE',
    };
    this.applyFilters();
  }

  onPageChange(newPage: number) {
    this.listSubfleets(newPage, this.limit);
  }

  onEdit(subfleet: Subfleet) {
    this.selectedSubfleet = { ...subfleet };
    this.showModal = true;
  }

  onCloseModal() {
    this.showModal = false;
  }

  onSaveModal(subfleet: Subfleet) {
    const id = subfleet.id;

    // Criar objeto apenas com campos permitidos para atualização
    const updateData = {
      name: subfleet.name,
      description: subfleet.description,
      color: subfleet.color,
      icon: subfleet.icon,
      parentId: subfleet.parentId,
      managerUserId: subfleet.managerUserId,
      status: subfleet.status,
    };

    this.serviceSubfleet.update(id!, updateData).subscribe({
      next: () => {
        this.listSubfleets(this.page, this.limit);
        this.loadIndicators();
        this.showModal = false;
        this.selectedSubfleet = undefined;
        alertSuccess('Subfrota atualizada com Sucesso');
      },
      error: (err) => {
        alertError(
          `Ocorreu um erro ao salvar a subfrota. ${
            err?.error?.message || 'Erro desconhecido.'
          }`
        );
      },
    });
  }

  onNavDetails(id?: number) {
    if (!id) return;
    this.router.navigate(['/frotas', id]);
  }
}
