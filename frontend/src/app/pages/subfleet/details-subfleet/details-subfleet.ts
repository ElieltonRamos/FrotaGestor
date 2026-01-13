import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';

import {
  Subfleet,
  SubfleetReport,
  SubfleetStatus,
} from '../../../interfaces/subfleet';
import { Vehicle } from '../../../interfaces/vehicle';

import { SubfleetService } from '../../../services/subfleet.service';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

import {
  BaseListComponent,
  ColumnConfig,
} from '../../../components/base-list-component/base-list-component';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';

@Component({
  selector: 'app-details-subfleet',
  standalone: true,
  imports: [CommonModule, DatePipe, BaseListComponent, PaginatorComponent],
  templateUrl: './details-subfleet.html',
})
export class DetailsSubfleet {
  private route = inject(ActivatedRoute);
  private service = inject(SubfleetService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  subfleet?: Subfleet;
  report?: SubfleetReport;

  loading = false;

  // Veículos da subfrota
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

  // frotas filhas
  childSubfleets: Subfleet[] = [];
  childColumns: ColumnConfig<any>[] = [
    { key: 'name', label: 'Nome', sortable: true },
    { key: 'status', label: 'Status', sortable: true },
    { key: 'managerName', label: 'Gerente' },
    { key: 'vehicleCount', label: 'Veículos' },
  ];
  childPage = 1;
  childLimit = 5;
  childTotal = 0;
  childTotalPages = 1;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.loadSubfleet(id);
    else this.router.navigate(['/frotas']);
  }

  private loadSubfleet(id: number) {
    this.loading = true;

    this.service.getById(id).subscribe({
      next: (res) => {
        this.subfleet = res;
        this.loading = false;
        this.cdr.detectChanges();

        this.loadReport(id);
        this.loadVehicles(id);
        this.loadChildSubfleets(id);
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/frotas']);
      },
    });
  }

  private loadReport(subfleetId: number) {
    this.service.getReport(subfleetId).subscribe({
      next: (res) => {
        this.report = res;
        this.cdr.detectChanges();
      },
      error: () => {
        this.report = undefined;
        this.cdr.detectChanges();
      },
    });
  }

  private loadVehicles(subfleetId: number) {
    this.service
      .getVehiclesBySubfleet(subfleetId, this.vehiclesPage, this.vehiclesLimit)
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
    if (this.subfleet?.id) this.loadVehicles(this.subfleet.id);
  }

  private loadChildSubfleets(parentId: number) {
    this.service
      .getChildSubfleets(parentId, this.childPage, this.childLimit)
      .subscribe({
        next: (res) => {
          this.childSubfleets = res.data;
          this.childTotal = res.total;
          this.childPage = res.page;
          this.childLimit = res.limit;
          this.childTotalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: () => {
          this.childSubfleets = [];
          this.childTotal = 0;
          this.cdr.detectChanges();
        },
      });
  }

  onChildPageChange(newPage: number) {
    this.childPage = newPage;
    if (this.subfleet?.id) this.loadChildSubfleets(this.subfleet.id);
  }

  onVehicleSelect(vehicle: any) {
    this.router.navigate(['/veiculos', vehicle.id]);
  }

  onChildSelect(subfleet: any) {
    this.router.navigate(['/frotas', subfleet.id]);
  }

  goBack() {
    this.router.navigate(['/frotas']);
  }

  disableSubfleet() {
    if (!this.subfleet?.id) return;

    this.loading = true;
    this.service.updateStatus(this.subfleet.id, 'INACTIVE').subscribe({
      next: () => {
        this.loadSubfleet(Number(this.route.snapshot.paramMap.get('id')));
        this.loading = false;
        this.cdr.detectChanges();
        alertSuccess('Subfrota desabilitada com sucesso.');
      },
      error: (err) => {
        this.loading = false;
        this.cdr.detectChanges();
        alertError(
          `Erro ao desabilitar a subfrota. ${err?.error?.message || ''}`
        );
      },
    });
  }

  activateSubfleet() {
    if (!this.subfleet?.id) return;

    this.loading = true;
    this.service.updateStatus(this.subfleet.id, 'ACTIVE').subscribe({
      next: () => {
        this.loadSubfleet(Number(this.route.snapshot.paramMap.get('id')));
        this.loading = false;
        this.cdr.detectChanges();
        alertSuccess('Subfrota reativada com sucesso.');
      },
      error: (err) => {
        this.loading = false;
        this.cdr.detectChanges();
        alertError(`Erro ao reativar a subfrota. ${err?.error?.message || ''}`);
      },
    });
  }

  isActive() {
    return this.subfleet?.status === SubfleetStatus.ACTIVE;
  }

  isInactive() {
    return this.subfleet?.status === SubfleetStatus.INACTIVE;
  }
}
