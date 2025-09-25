import {
  Component,
  EventEmitter,
  Input,
  Output,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  BaseListComponent,
  ColumnConfig,
} from '../base-list-component/base-list-component';
import {
  BaseFilterComponent,
  FilterConfig,
} from '../base-filter-component/base-filter-component';
import { PaginatorComponent } from '../paginator/paginator.component';
import { PaginatedResponse } from '../../interfaces/paginator';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-select-modal',
  standalone: true,
  imports: [
    CommonModule,
    BaseListComponent,
    BaseFilterComponent,
    PaginatorComponent,
  ],
  templateUrl: './select-modal.component.html',
})
export class SelectModalComponent<T extends { id?: number }> {
  @Input() show = false;
  @Input() title = 'Selecionar';
  @Input() columns: ColumnConfig<T>[] = [];
  @Input() filtersConfig: FilterConfig[] = [];
  @Input() dataFetcher!: (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof T,
    sortAsc: boolean
  ) => Observable<PaginatedResponse<T>>;

  @Output() select = new EventEmitter<T>();
  @Output() close = new EventEmitter<void>();

  data: T[] = [];
  filter: any = {};
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 1;
  sortKey: keyof T = 'id' as keyof T;
  sortAsc = true;

  ngOnChanges(changes: SimpleChanges) {
    if (changes['show']?.currentValue === true) {
      this.page = 1;
      this.loadData();
    }
  }

  loadData() {
    const appliedFilters = { ...this.filter, status: 'ATIVO' };
    if (!this.dataFetcher) return;
    this.dataFetcher(
      this.page,
      this.limit,
      appliedFilters,
      this.sortKey,
      this.sortAsc
    ).subscribe({
      next: (res) => {
        this.data = res.data;
        this.total = res.total;
        this.page = res.page;
        this.limit = res.limit;
        this.totalPages = res.totalPages;
      },
    });
  }

  applyFilters() {
    this.page = 1;
    this.loadData();
  }

  clearFilters() {
    this.filter = {};
    this.applyFilters();
  }

  onPageChange(newPage: number) {
    this.page = newPage;
    this.loadData();
  }

  onSort(event: any) {
    this.sortKey = event.key;
    this.sortAsc = event.asc;
    this.loadData();
  }

  onSelect(item: any) {
    this.select.emit(item);
    this.onClose();
  }

  onClose() {
    this.close.emit();
  }
}
