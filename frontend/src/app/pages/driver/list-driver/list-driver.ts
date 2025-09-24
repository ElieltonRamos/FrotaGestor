import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Driver } from '../../../interfaces/driver';
import { DriverService } from '../../../services/driver.service';
import { PaginatedResponse } from '../../../interfaces/paginator';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';
import { ModalEditComponent } from '../../../components/modal-edit-component/modal-edit-component';
import { Router } from '@angular/router';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { CommonModule, DatePipe } from '@angular/common';

@Component({
  selector: 'app-list-driver',
  imports: [FormsModule, PaginatorComponent, ModalEditComponent, CommonModule, DatePipe],
  templateUrl: './list-driver.html',
})
export class ListDriver {
  private serviceDriver = inject(DriverService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  driverFields = [
    { name: 'name', label: 'Nome', type: 'text' },
    { name: 'cpf', label: 'CPF', type: 'text' },
    { name: 'cnh', label: 'CNH', type: 'text' },
    { name: 'cnhCategory', label: 'Categoria CNH', type: 'text' },
    { name: 'cnhExpiration', label: 'Validade CNH', type: 'date' },
    {
      name: 'status',
      label: 'Status',
      type: 'select',
      options: ['ATIVO', 'INATIVO'],
    },
    { name: 'email', label: 'Email', type: 'email' },
    { name: 'phone', label: 'Telefone', type: 'text' },
  ];

  drivers: Driver[] = [];
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 1;
  selectedDriver?: Driver;
  showModal = false;

  // filtros
  filter = {
    name: '',
    cpf: '',
    status: 'ATIVO',
  };

  // ordenação
  sortKey: keyof Driver = 'name';
  sortAsc = true;

  ngOnInit() {
    this.listDrivers(1, 10);
  }

  listDrivers(page: number, limit: number) {
    this.serviceDriver
      .getAll(page, limit, this.filter, this.sortKey, this.sortAsc)
      .subscribe({
        next: (res: PaginatedResponse<Driver>) => {
          this.drivers = res.data;
          this.total = res.total;
          this.page = res.page;
          this.limit = res.limit;
          this.totalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.log('Erro ao carregar motoristas:', err);
          this.drivers = [];
          this.total = 0;
          this.totalPages = 0;
        },
      });
  }

  applyFilters() {
    this.page = 1;
    this.listDrivers(this.page, this.limit);
    this.cdr.detectChanges();
  }

  sortBy(key: keyof Driver) {
    if (this.sortKey === key) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortKey = key;
      this.sortAsc = true;
    }
    this.listDrivers(this.page, this.limit);
  }

  clearFilters() {
    this.filter = { name: '', cpf: '', status: '' };
    this.applyFilters();
  }

  onPageChange(newPage: number) {
    this.listDrivers(newPage, this.limit);
  }

  onEdit(driver: Driver) {
    this.selectedDriver = { ...driver };
    this.showModal = true;
  }

  onCloseModal() {
    this.showModal = false;
  }

  onSaveModal(driver: Driver) {
    const id = driver.id;
    delete driver.id;
    delete driver.deletedAt;

    this.serviceDriver.update(id!, driver).subscribe({
      next: () => {
        this.listDrivers(1, 10);
        this.showModal = false;
        this.selectedDriver = undefined;
        alertSuccess('Motorista atualizado com Sucesso');
      },
      error: (err) => {
        alertError(
          `Ocorreu um erro ao salvar o motorista. ${
            err?.error?.message || 'Erro desconhecido.'
          }`
        );
      },
    });
  }

  onNavDetails(id?: number) {
    if (!id) return;
    this.router.navigate(['/motoristas', id]);
  }
}
