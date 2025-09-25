import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { alertError } from '../../utils/custom-alerts';
import { mapNetworkError } from '../../services/api.url';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.html',
})
export class Login {
  username = '';
  password = '';
  isLoading = false;

  private router = inject(Router);
  private userService = inject(UserService);
  private cdr = inject(ChangeDetectorRef);

  login() {
    this.isLoading = true;

    this.userService.login(this.username, this.password).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.cdr.detectChanges();
        localStorage.setItem('auth_token', res.token);
        this.router.navigate(['/menu']);
      },
      error: (err) => {
        this.isLoading = false;
        this.cdr.detectChanges();
        const message = mapNetworkError(err, 'Erro ao fazer login');
        alertError(message);
      },
    });
  }
}
