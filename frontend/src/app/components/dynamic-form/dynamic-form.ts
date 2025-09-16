import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';

export interface FormField {
  name: string;         // nome do campo (para o formControlName)
  label: string;        // texto que aparece no formulário
  type: string;         // text, email, number, date, select, etc
  placeholder?: string; // placeholder
  options?: string[];   // se for select/radio
  required?: boolean;   // validação
}

@Component({
  selector: 'app-dynamic-form',
  imports: [FormsModule, ReactiveFormsModule],
  templateUrl: './dynamic-form.html'
})
export class DynamicFormComponent {
  @Input() fields: FormField[] = [];
  @Output() formSubmit = new EventEmitter<any>();

  form!: FormGroup;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    const group: any = {};
    this.fields.forEach(field => {
      group[field.name] = field.required
        ? [null, Validators.required]
        : [null];
    });
    this.form = this.fb.group(group);
  }

  onSubmit() {
    if (this.form.valid) {
      this.formSubmit.emit(this.form.value);
    }
  }
}
