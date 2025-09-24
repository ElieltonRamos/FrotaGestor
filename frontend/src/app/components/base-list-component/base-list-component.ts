import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface ColumnConfig<T> {
  key: keyof T;
  label: string;
  sortable?: boolean;
  type?: 'text' | 'date' | 'status';
}

@Component({
  selector: 'app-base-list',
  imports: [CommonModule],
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

  @Output() sortChange = new EventEmitter<keyof T>();
  @Output() pageChange = new EventEmitter<number>();
  @Output() edit = new EventEmitter<T>();
  @Output() details = new EventEmitter<number>();

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

  formatDate(value: unknown): Date | null {
    if (!value) return null;
    return new Date(value as any);
  }
}
