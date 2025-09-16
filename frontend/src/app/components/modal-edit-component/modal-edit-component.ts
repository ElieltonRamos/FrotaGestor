import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormField } from '../dynamic-form/dynamic-form';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-modal-edit-component',
  imports: [FormsModule],
  templateUrl: './modal-edit-component.html',
  styles: ``,
})
export class ModalEditComponent {
  @Input() title: string = 'Editar';
  @Input() show: boolean = false;
  @Input() entity: any = {};
  @Input() fields: FormField[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<any>();

  onClose() {
    this.close.emit();
  }

  onSave() {
    this.save.emit(this.entity);
  }
}
