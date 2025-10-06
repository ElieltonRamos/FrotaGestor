import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ColumnConfig,
  BaseListComponent,
} from '../../../components/base-list-component/base-list-component';
import {
  FilterConfig,
  BaseFilterComponent,
} from '../../../components/base-filter-component/base-filter-component';
import { UserService } from '../../../services/user.service';
import { PaginatedResponse } from '../../../interfaces/paginator';
import User, { UserIndicators, UserRole } from '../../../interfaces/user';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { ModalEditComponent } from '../../../components/modal-edit-component/modal-edit-component';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';
import { Router } from '@angular/router';

@Component({
  selector: 'app-list-user',
  imports: [
    CommonModule,
    FormsModule,
    ModalEditComponent,
    BaseListComponent,
    PaginatorComponent,
    BaseFilterComponent,
  ],
  templateUrl: './list-users.html',
})
export class ListUsers {
  private serviceUser = inject(UserService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  users: User[] = [];
  total = 0;
  limit = 10;
  page = 1;
  totalPages = 1;
  sortKey: keyof User = 'id';
  sortAsc = true;
  showModal = false;
  selectedUser?: User;
  loadingIndicators = false;

  indicators?: UserIndicators;

  filter = {
    id: '',
    username: '',
    role: '',
  };

  userColumns: ColumnConfig<User>[] = [
    { key: 'id', label: 'ID', sortable: true },
    { key: 'username', label: 'Usuário', sortable: true },
    { key: 'role', label: 'Função', sortable: true },
  ];

  userFilters = [
    { key: 'id', label: 'ID', type: 'number', placeholder: 'ID...' },
    { key: 'username', label: 'Usuário', type: 'text', placeholder: 'Nome...' },
    {
      key: 'role',
      label: 'Função',
      type: 'select',
      options: [UserRole.ADMIN, UserRole.USER],
    },
  ] satisfies FilterConfig[];

  userFields = [
    { name: 'username', label: 'Usuário', type: 'text' },
    { name: 'password', label: 'Senha', type: 'password' },
    {
      name: 'role',
      label: 'Função',
      type: 'select',
      options: [UserRole.ADMIN, UserRole.USER],
    },
  ];

  ngOnInit() {
    this.listUsers(1, 10);
    this.loadIndicators();
  }

  loadIndicators() {
    this.loadingIndicators = true;
    this.serviceUser.getIndicators().subscribe({
      next: (res) => {
        this.indicators = res;
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.indicators = {
          totalUsers: 0,
          admins: 0,
          regulars: 0,
        };
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
    });
  }

  listUsers(page: number, limit: number) {
    this.serviceUser
      .getAll(page, limit, this.filter, this.sortKey, this.sortAsc)
      .subscribe({
        next: (res: PaginatedResponse<User>) => {
          this.users = res.data;
          this.total = res.total;
          this.page = res.page;
          this.limit = res.limit;
          this.totalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: () => {
          this.users = [];
          this.total = 0;
          this.totalPages = 0;
        },
      });
  }

  applyFilters() {
    this.page = 1;
    this.listUsers(this.page, this.limit);
    this.loadIndicators();
  }

  clearFilters() {
    this.filter = { id: '', username: '', role: '' };
    this.applyFilters();
  }

  sortBy(key: keyof User) {
    if (this.sortKey === key) this.sortAsc = !this.sortAsc;
    else {
      this.sortKey = key;
      this.sortAsc = true;
    }
    this.listUsers(this.page, this.limit);
  }

  onPageChange(newPage: number) {
    this.listUsers(newPage, this.limit);
  }

  onEdit(user: User) {
    this.selectedUser = { ...user }
    this.showModal = true;
  }

  onCloseModal() {
    this.showModal = false;
  }

  onSaveModal(user: User) {
    console.log(user);
    const id = user.id;

    if (user.password === '') {
      delete user.password;
    }

    this.serviceUser.update(id!, user).subscribe({
      next: () => {
        this.listUsers(1, 10);
        this.loadIndicators();
        this.showModal = false;
        this.selectedUser = undefined;
        alertSuccess('Usuário atualizado com sucesso!');
      },
      error: (err) => {
        alertError(
          `Erro ao salvar o usuário: ${
            err?.error?.message || 'Erro desconhecido.'
          }`
        );
      },
    });
  }

  onDetails(id?: number) {
    if (!id) return;
    this.router.navigate(['/usuarios', id]);
  }
}
