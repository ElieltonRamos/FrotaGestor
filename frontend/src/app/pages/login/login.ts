import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../services/user-service';
import { alertError } from '../../utils/custom-alerts';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
})
export class Login {
  username = '';
  password = '';
  private router = inject(Router);
  private userService = inject(UserService);

  login() {
    this.userService.login(this.username, this.password).subscribe({
      next: () => this.router.navigate(['/menu']),
      error: (err) => alertError(`Erro ao fazer login: ${err.message}`),
    });
  }
}
