import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface FilterConfig {
  key: string;
  label: string;
  type: 'text' | 'number' | 'select';
  placeholder?: string;
  options?: string[];
}

@Component({
  selector: 'app-base-filter',
  imports: [CommonModule, FormsModule],
  templateUrl: './base-filter-component.html',
})
export class BaseFilterComponent {
  @Input() filtersConfig: FilterConfig[] = [];
  @Input() filter: Record<string, any> = {};
  @Output() filterChange = new EventEmitter<Record<string, any>>();
  @Output() apply = new EventEmitter<void>();
  @Output() clear = new EventEmitter<void>();

  onInputChange() {
    this.filterChange.emit(this.filter);
  }

  onApply() {
    this.apply.emit();
  }

  onClear() {
    this.filter = {};
    this.clear.emit();
    this.filterChange.emit(this.filter);
  }
}
