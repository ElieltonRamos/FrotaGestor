import { Component, Input, Output, EventEmitter } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  FormsModule,
  ReactiveFormsModule,
} from '@angular/forms';

export interface FormField {
  name: string;
  label: string;
  type: string;
  placeholder?: string;
  options?: string[];
  required?: boolean;
}

@Component({
  selector: 'app-dynamic-form',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule],
  templateUrl: './dynamic-form.html',
})
export class DynamicFormComponent {
  @Input() fields: FormField[] = [];
  @Output() formSubmit = new EventEmitter<any>();
  @Output() formChange = new EventEmitter<any>();

  form!: FormGroup;
  dropdownOpen: { [key: string]: boolean } = {};

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    const group: any = {};
    this.fields.forEach((field) => {
      group[field.name] = field.required ? [null, Validators.required] : [null];
      this.dropdownOpen[field.name] = false; // Initialize dropdown state
    });
    this.form = this.fb.group(group);
    this.form.valueChanges.subscribe((value) => {
      this.formChange.emit(value);
    });
  }

  onSubmit() {
    if (this.form.valid) {
      this.formSubmit.emit(this.form.value);
      this.form.reset();
      Object.keys(this.dropdownOpen).forEach((key) => {
        this.dropdownOpen[key] = false;
      });
    } else {
      this.form.markAllAsTouched();
    }
  }

  toggleDropdown(fieldName: string) {
    this.dropdownOpen[fieldName] = !this.dropdownOpen[fieldName];
  }

  selectOption(fieldName: string, option: string) {
    this.form.get(fieldName)?.setValue(option);
    this.dropdownOpen[fieldName] = false;
  }
}
