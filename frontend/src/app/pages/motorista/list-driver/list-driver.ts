import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Driver } from '../../../interfaces/driver';
import { DriverService } from '../../../services/driver-service';
import { PaginatedResponse } from '../../../interfaces/paginator';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';
import { ModalEditComponent } from '../../../components/modal-edit-component/modal-edit-component';

@Component({
  selector: 'app-list-driver',
  standalone: true,
  imports: [FormsModule, PaginatorComponent, ModalEditComponent],
  templateUrl: './list-driver.html',
})
export class ListDriver {
  private serviceDriver = inject(DriverService);
  driverFields = [
    { name: 'nome', label: 'Nome', type: 'text', required: true },
    { name: 'cpf', label: 'CPF', type: 'text', required: true },
    { name: 'cnh', label: 'CNH', type: 'text', required: true },
    {
      name: 'status',
      label: 'Status',
      type: 'select',
      options: ['Ativo', 'Inativo'],
    },
    { name: 'email', label: 'Email', type: 'email' },
    { name: 'telefone', label: 'Telefone', type: 'text' },
  ];

  drivers: Driver[] = [];
  total = 0;
  page = 1;
  limit = 5;
  totalPages = 0;
  selectedDriver?: Driver;
  showModal = false;

  // filtros
  filter = {
    nome: '',
    cpf: '',
    status: '',
  };

  // ordenação
  sortKey: keyof Driver = 'nome';
  sortAsc = true;

  ngOnInit(): void {
    this.drivers = [
      {
        id: 1,
        nome: 'John Doe',
        cpf: '123.456.789-00',
        cnh: '123456789012345678',
        status: 'Ativo',
        categoriaCnh: 'AB',
        email: 'teste@email.com',
        telefone: '38988663580',
      },
    ];
  }

  listDrivers(page: number = this.page, limit: number = this.limit) {
    this.serviceDriver
      .getAll(page, limit, this.filter, this.sortKey, this.sortAsc)
      .subscribe((res: PaginatedResponse<Driver>) => {
        this.drivers = res.data;
        this.total = res.total;
        this.page = res.page;
        this.limit = res.limit;
        this.totalPages = res.totalPages;
      });
  }

  applyFilters() {
    this.page = 1;
    this.listDrivers();
  }

  sortBy(key: keyof Driver) {
    if (this.sortKey === key) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortKey = key;
      this.sortAsc = true;
    }
    this.listDrivers();
  }

  clearFilters() {
    this.filter = { nome: '', cpf: '', status: '' };
    this.applyFilters();
  }

  onPageChange(newPage: number) {
    this.listDrivers(newPage);
  }

  toggleStatus(driver: Driver) {
    driver.status = driver.status === 'Ativo' ? 'Inativo' : 'Ativo';
  }

  onEdit(driver: Driver) {
    this.selectedDriver = { ...driver };
    this.showModal = true;
  }

  onCloseModal() {
    this.showModal = false;
  }

  onSaveModal(driver: Driver) {
    this.serviceDriver.update(driver.id!, driver).subscribe(() => {
      const index = this.drivers.findIndex((d) => d.id === driver.id);
      if (index !== -1) this.drivers[index] = { ...driver };
      this.showModal = false;
      this.selectedDriver = undefined;
    });
  }
}
