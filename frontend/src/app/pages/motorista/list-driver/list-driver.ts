import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Driver } from '../../../interfaces/driver';
import { DriverService } from '../../../services/driver-service';
import { PaginatedResponse } from '../../../interfaces/paginator';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';

@Component({
  selector: 'app-list-driver',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginatorComponent],
  templateUrl: './list-driver.html',
})
export class ListDriver implements OnInit {
  private serviceDriver = inject(DriverService);

  drivers: Driver[] = [];
  total = 0;
  page = 1;
  limit = 5;
  totalPages = 0;

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
    this.listDrivers();
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

  onEdit(driver: Driver) {
    console.log('Editar motorista:', driver);
  }

  toggleStatus(driver: Driver) {
    driver.status = driver.status === 'Ativo' ? 'Inativo' : 'Ativo';
  }
}
