import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { VehicleService } from '../../../services/vehicle.service';
import { Vehicle, VehicleStatus } from '../../../interfaces/vehicle';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { MapComponent } from '../../../components/map-component/map-component';
import {
  BaseListComponent,
  ColumnConfig,
} from '../../../components/base-list-component/base-list-component';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';
import { CommonModule } from '@angular/common';
import { Driver } from '../../../interfaces/driver';
import { Trip } from '../../../interfaces/trip';
import { Expense } from '../../../interfaces/expense';

@Component({
  selector: 'app-details-vehicle',
  standalone: true,
  imports: [CommonModule, MapComponent, BaseListComponent, PaginatorComponent],
  templateUrl: './details-vehicle.html',
})
export class DetailsVehicle {
  private route = inject(ActivatedRoute);
  private serviceVehicle = inject(VehicleService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  vehicle?: Vehicle;
  loading = false;
  topDriver?: Driver;

  trips: Trip[] = [];
  tripsColumns: ColumnConfig<any>[] = [
    { key: 'startTime', label: 'Data de Inicio', sortable: true },
    { key: 'startLocation', label: 'Origem' },
    { key: 'endLocation', label: 'Destino' },
    { key: 'driverName', label: 'Motorista' },
  ];
  tripsPage = 1;
  tripsLimit = 5;
  tripsTotal = 0;
  tripsTotalPages = 1;

  expenses: Expense[] = []
  expensesColumns: ColumnConfig<any>[] = [
    { key: 'date', label: 'Data', sortable: true },
    { key: 'description', label: 'Descrição' },
    { key: 'amount', label: 'Valor', sortable: true },
    { key: 'category', label: 'Categoria' },
  ];
  expensesPage = 1;
  expensesLimit = 5;
  expensesTotal = 0;
  expensesTotalPages = 1;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.loadVehicle(id);
    else this.router.navigate(['/veiculos']);
  }

  private loadVehicle(id: number) {
    this.loading = true;
    this.serviceVehicle.getById(id).subscribe({
      next: (res) => {
        this.vehicle = res;
        this.loading = false;
        this.cdr.detectChanges();
        this.loadTopDriver(id);
        this.loadTrips(id);
        // this.loadExpenses(id);
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/veiculos']);
      },
    });
  }

  private loadTopDriver(vehicleId: number) {
    this.serviceVehicle.getTopDriverByVehicle(vehicleId).subscribe({
      next: (driver) => {
        this.topDriver = driver;
        this.cdr.detectChanges();
      },
    });
  }

  // 🔹 Viagens
  private loadTrips(vehicleId: number) {
    this.serviceVehicle
      .getTripsByVehicle(vehicleId, this.tripsPage, this.tripsLimit)
      .subscribe({
        next: (res) => {
          this.trips = res.data;
          this.tripsTotal = res.total;
          this.tripsPage = res.page;
          this.tripsLimit = res.limit;
          this.tripsTotalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: () => {
          this.trips = [];
          this.tripsTotal = 0;
        },
      });
  }

  onTripsPageChange(newPage: number) {
    this.tripsPage = newPage;
    if (this.vehicle?.id) this.loadTrips(this.vehicle.id);
  }

  private loadExpenses(vehicleId: number) {
    this.serviceVehicle
      .getExpensesByVehicle(vehicleId, this.expensesPage, this.expensesLimit)
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
        },
      });
  }

  onExpensesPageChange(newPage: number) {
    this.expensesPage = newPage;
    if (this.vehicle?.id) this.loadExpenses(this.vehicle.id);
  }

  // 🔹 Interações
  onTripSelect(trip: any) {
    this.router.navigate(['/viagens', trip.id]);
  }

  onExpenseSelect(expense: any) {
    this.router.navigate(['/despesas', expense.id]);
  }

  goBack() {
    this.router.navigate(['/veiculos']);
  }

  disableVehicle() {
    if (!this.vehicle?.id) return;
    this.loading = true;

    this.serviceVehicle
      .update(this.vehicle.id, { status: VehicleStatus.INATIVO })
      .subscribe({
        next: () => {
          this.loadVehicle(this.vehicle!.id!);
          this.loading = false;
          alertSuccess('Veículo desabilitado com sucesso.');
        },
        error: (err) => {
          this.loading = false;
          alertError(
            `Erro ao desabilitar o veículo. ${err?.error?.message || ''}`
          );
        },
      });
  }

  activateVehicle() {
    if (!this.vehicle?.id) return;
    this.loading = true;

    this.serviceVehicle
      .update(this.vehicle.id, { status: VehicleStatus.ATIVO })
      .subscribe({
        next: () => {
          this.loadVehicle(this.vehicle!.id!);
          this.loading = false;
          alertSuccess('Veículo reativado com sucesso.');
        },
        error: (err) => {
          this.loading = false;
          alertError(
            `Erro ao reativar o veículo. ${err?.error?.message || ''}`
          );
        },
      });
  }
}
