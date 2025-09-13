import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
})
export class Login {
  username = '';
  password = '';
  private router = inject(Router);

  login() {
    console.log('Usuário:', this.username);
    console.log('Senha:', this.password);
    this.router.navigate(['/menu']);
  }
}
