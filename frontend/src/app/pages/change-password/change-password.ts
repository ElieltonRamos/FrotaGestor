import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, ValidationErrors, AbstractControl } from '@angular/forms';
import { alertError, alertSuccess } from '../../utils/custom-alerts';
import { UserService } from '../../services/user-service';

@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule],
  templateUrl: './change-password.html',
})
export class ChangePassword {
  private formBuilder = inject(FormBuilder);
  private userService = inject(UserService);

  form = this.formBuilder.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: this.passwordsMatchValidator }
  );

  private passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (newPassword && confirmPassword && newPassword !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  onSubmit() {
    if (this.form.invalid) {
      alertError('Corrija os erros antes de continuar.');
      return;
    }

    const { newPassword } = this.form.value;
    this.userService.changePassword(newPassword!).subscribe({
      next: () => {
        this.clear();
        alertSuccess('Senha alterada com sucesso');
      },
      error: (err) => alertError(`Erro ao alterar senha: ${err.error.message}`),
    });
  }

  clear() {
    this.form.reset();
  }

  isInvalid(controlName: string) {
    const control = this.form.get(controlName);
    return control?.invalid && (control.dirty || control.touched);
  }

  hasError(errorCode: string) {
    return this.form.errors?.[errorCode];
  }
}
