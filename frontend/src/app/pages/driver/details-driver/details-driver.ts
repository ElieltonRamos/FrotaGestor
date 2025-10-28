import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { Driver } from '../../../interfaces/driver';
import { ActivatedRoute, Router } from '@angular/router';
import { DriverService } from '../../../services/driver.service';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { CommonModule, DatePipe } from '@angular/common';
import { Vehicle } from '../../../interfaces/vehicle';
import { Expense } from '../../../interfaces/expense';
import {
  BaseListComponent,
  ColumnConfig,
} from '../../../components/base-list-component/base-list-component';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';

@Component({
  selector: 'app-details-driver',
  standalone: true,
  imports: [CommonModule, DatePipe, BaseListComponent, PaginatorComponent],
  templateUrl: './details-driver.html',
})
export class DetailsDriver {
  private route = inject(ActivatedRoute);
  private serviceDriver = inject(DriverService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  driver?: Driver;
  loading = false;

  vehicles: Vehicle[] = [];
  vehiclesColumns: ColumnConfig<any>[] = [
    { key: 'plate', label: 'Placa', sortable: true },
    { key: 'model', label: 'Modelo' },
    { key: 'brand', label: 'Marca' },
    { key: 'year', label: 'Ano' },
  ];
  vehiclesPage = 1;
  vehiclesLimit = 5;
  vehiclesTotal = 0;
  vehiclesTotalPages = 1;

  expenses: Expense[] = [];
  expensesColumns: ColumnConfig<any>[] = [
    { key: 'date', label: 'Data', sortable: true },
    { key: 'description', label: 'Descrição' },
    { key: 'amount', label: 'Valor', sortable: true },
    { key: 'type', label: 'Tipo' },
  ];
  expensesPage = 1;
  expensesLimit = 5;
  expensesTotal = 0;
  expensesTotalPages = 1;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadDriver(id);
    } else {
      this.router.navigate(['/motoristas']);
    }
  }

  private loadDriver(id: number) {
    this.loading = true;
    this.serviceDriver.getById(id).subscribe({
      next: (res) => {
        this.driver = res;
        this.loading = false;
        this.cdr.detectChanges();
        this.loadVehicles(id);
        this.loadExpenses(id);
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/motoristas']);
      },
    });
  }

  private loadVehicles(driverId: number) {
    this.serviceDriver
      .getVehiclesByDriver(driverId, this.vehiclesPage, this.vehiclesLimit)
      .subscribe({
        next: (res) => {
          this.vehicles = res.data;
          this.vehiclesTotal = res.total;
          this.vehiclesPage = res.page;
          this.vehiclesLimit = res.limit;
          this.vehiclesTotalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: () => {
          this.vehicles = [];
          this.vehiclesTotal = 0;
          this.cdr.detectChanges();
        },
      });
  }

  onVehiclesPageChange(newPage: number) {
    this.vehiclesPage = newPage;
    if (this.driver?.id) this.loadVehicles(this.driver.id);
  }

  private loadExpenses(driverId: number) {
    this.serviceDriver
      .getExpensesByDriver(driverId, this.expensesPage, this.expensesLimit)
      .subscribe({
        next: (res) => {
          this.expenses = res.data;
          this.expensesTotal = res.total;
          this.expensesPage = res.page;
          this.expensesLimit = res.limit;
          this.expensesTotalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: () => {
          this.expenses = [];
          this.expensesTotal = 0;
          this.cdr.detectChanges();
        },
      });
  }

  onExpensesPageChange(newPage: number) {
    this.expensesPage = newPage;
    if (this.driver?.id) this.loadExpenses(this.driver.id);
  }

  onVehicleSelect(vehicle: any) {
    this.router.navigate(['/veiculos', vehicle.id]);
  }

  onExpenseSelect(expense: any) {
    this.router.navigate(['/despesas', expense.id]);
  }

  goBack() {
    this.router.navigate(['/motoristas']);
  }

  disableDriver() {
    if (!this.driver || !this.driver.id) return;
    this.loading = true;

    this.serviceDriver.delete(this.driver.id).subscribe({
      next: () => {
        this.loadDriver(Number(this.route.snapshot.paramMap.get('id')));
        this.loading = false;
        this.cdr.detectChanges();
        alertSuccess('Motorista desabilitado com sucesso.');
      },
      error: (err) => {
        this.loading = false;
        this.cdr.detectChanges();
        alertError(
          `Erro ao desabilitar o motorista. ${err.error.message || ''}`
        );
      },
    });
  }

  activateDriver() {
    if (!this.driver || !this.driver.id) return;

    this.loading = true;

    this.serviceDriver.update(this.driver.id, { status: 'ATIVO' }).subscribe({
      next: () => {
        this.loadDriver(Number(this.route.snapshot.paramMap.get('id')));
        this.loading = false;
        this.cdr.detectChanges();
        alertSuccess('Motorista reativado com sucesso.');
      },
      error: (err) => {
        this.loading = false;
        this.cdr.detectChanges();
        alertError(`Erro ao reativar o motorista. ${err.error.message || ''}`);
      },
    });
  }
}
