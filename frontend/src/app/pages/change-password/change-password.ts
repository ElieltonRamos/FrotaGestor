import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { alertError, alertSuccess } from '../../utils/custom-alerts';
import { UserService } from '../../services/user-service';

@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule],
  templateUrl: './change-password.html',
  styles: ``,
})
export class ChangePassword {
  private formBuilder = inject(FormBuilder);
  private userService = inject(UserService);

  form = this.formBuilder.group({
    newPassword: ['', [Validators.required]],
    confirmPassword: ['', [Validators.required]],
  });

  onSubmit() {
    if (this.form.invalid) return;
    const { newPassword, confirmPassword } = this.form.value;

    if (newPassword !== confirmPassword) {
      alertError('As senhas não conferem');
      return;
    }

    const newPasswordConfirmed = newPassword || '';

    this.userService.changePassword(newPasswordConfirmed).subscribe({
      next: (res) => alertSuccess('Senha alterada com sucesso'),
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
}
