import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
})
export class Login {
  username = '';
  password = '';

  login() {
    console.log('Usuário:', this.username);
    console.log('Senha:', this.password);
    // aqui você implementa a lógica real de autenticação
  }
}
