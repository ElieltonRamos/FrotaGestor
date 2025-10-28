import { Component, inject } from '@angular/core';
import { DynamicFormComponent } from '../../../components/dynamic-form/dynamic-form';
import { UserService } from '../../../services/user.service';
import User, { UserRole } from '../../../interfaces/user';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { Message } from '../../../interfaces/user';

@Component({
  selector: 'app-create-users',
  imports: [DynamicFormComponent],
  templateUrl: './create-users.html',
  styles: ``,
})
export class CreateUser {
  private userService = inject(UserService);

  userFields = [
    {
      name: 'username',
      label: 'Usuário',
      type: 'text',
      placeholder: 'Digite o nome de usuário',
      required: true,
    },
    {
      name: 'password',
      label: 'Senha',
      type: 'text',
      placeholder: 'Digite a senha',
      required: true,
    },
    {
      name: 'role',
      label: 'Função',
      type: 'select',
      options: [UserRole.ADMIN, UserRole.USER],
      required: true,
      placeholder: 'Selecione a função',
    },
  ];

  saveUser(data: User) {
    if (!data.username || !data.password || !data.role) {
      alertError('Preencha todos os campos obrigatórios!');
      return;
    }

    this.userService.create(data).subscribe({
      next: (res: Message) => {
        alertSuccess('Usuário criado com sucesso!');
      },
      error: (err) => {
        alertError(
          `Erro ao criar usuário: ${
            err?.error?.message || 'Erro desconhecido.'
          }`
        );
      },
    });
  }
}
