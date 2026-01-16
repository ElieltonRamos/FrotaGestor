import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';

import { Subfleet, SubfleetReport } from '../../../interfaces/subfleet';
import { Vehicle } from '../../../interfaces/vehicle';

import { SubfleetService } from '../../../services/subfleet.service';
import { alertError } from '../../../utils/custom-alerts';

import {
  BaseListComponent,
  ColumnConfig,
} from '../../../components/base-list-component/base-list-component';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';

@Component({
  selector: 'app-details-subfleet',
  standalone: true,
  imports: [CommonModule, BaseListComponent, PaginatorComponent],
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

  onVehicleSelect(vehicle: any) {
    this.router.navigate(['/veiculos', vehicle.id]);
  }

  goBack() {
    this.router.navigate(['/frotas']);
  }
}
