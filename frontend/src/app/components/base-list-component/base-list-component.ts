import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrDatePipe } from '../../utils/date.pipe';

export interface ColumnConfig<T> {
  key: keyof T;
  label: string;
  sortable?: boolean;
  type?: 'text' | 'date' | 'status';
}

@Component({
  selector: 'app-base-list',
  imports: [CommonModule, BrDatePipe],
  templateUrl: './base-list-component.html',
})
export class BaseListComponent<T extends { id?: number }> {
  @Input() columns: ColumnConfig<T>[] = [];
  @Input() data: T[] = [];
  @Input() page = 1;
  @Input() limit = 10;
  @Input() total = 0;
  @Input() totalPages = 1;
  @Input() sortKey?: keyof T;
  @Input() sortAsc = true;
  @Input() showActions = true;

  @Output() sortChange = new EventEmitter<keyof T>();
  @Output() pageChange = new EventEmitter<number>();
  @Output() edit = new EventEmitter<T>();
  @Output() details = new EventEmitter<number>();
  @Output() rowClick = new EventEmitter<T>();

  onRowClick(item: T) {
    this.rowClick.emit(item);
  }

  onSort(key: keyof T) {
    this.sortChange.emit(key);
  }

  onPageChange(newPage: number) {
    this.pageChange.emit(newPage);
  }

  onEdit(item: T) {
    this.edit.emit(item);
  }

  onDetails(id?: number) {
    if (id) this.details.emit(id);
  }

  safeDateString(value: unknown): string | null {
    if (typeof value === 'string') {
      const dateStr = value.trim();
      // Só adiciona T00 para YYYY-MM-DD puro
      if (dateStr.match(/^(\d{4}-\d{2}-\d{2})$/)) {
        return dateStr + 'T00:00:00';
      }
      return dateStr; // Outros já OK
    }
    return null;
  }
}
