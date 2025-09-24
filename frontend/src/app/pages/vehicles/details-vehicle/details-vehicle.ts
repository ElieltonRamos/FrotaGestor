import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { Vehicle, VehicleStatus } from '../../../interfaces/vehicle';
import { ActivatedRoute, Router } from '@angular/router';
import { VehicleService } from '../../../services/vehicle.service';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

@Component({
  selector: 'app-details-vehicle',
  imports: [],
  templateUrl: './details-vehicle.html',
  styles: ``,
})
export class DetailsVehicle {
  private route = inject(ActivatedRoute);
  private serviceVehicle = inject(VehicleService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  vehicle?: Vehicle;
  loading = false;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadVehicle(id);
    } else {
      this.router.navigate(['/veiculos']);
    }
  }

  private loadVehicle(id: number) {
    this.loading = true;
    this.serviceVehicle.getById(id).subscribe({
      next: (res) => {
        this.vehicle = res;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/veiculos']);
      },
    });
  }

  goBack() {
    this.router.navigate(['/veiculos']);
  }

  disableVehicle() {
    if (!this.vehicle || !this.vehicle.id) return;
    this.loading = true;

    this.serviceVehicle.update(this.vehicle.id, { status: VehicleStatus.INATIVO }).subscribe({
      next: () => {
        this.loadVehicle(Number(this.route.snapshot.paramMap.get('id')));
        this.loading = false;
        this.cdr.detectChanges();
        alertSuccess('Veículo desabilitado com sucesso.');
      },
      error: (err) => {
        this.loading = false;
        this.cdr.detectChanges();
        alertError(`Erro ao desabilitar o veículo. ${err?.error?.message || ''}`);
      },
    });
  }

  activateVehicle() {
    if (!this.vehicle || !this.vehicle.id) return;

    this.loading = true;

    this.serviceVehicle.update(this.vehicle.id, { status: VehicleStatus.ATIVO }).subscribe({
      next: () => {
        this.loading = false;
        this.loadVehicle(Number(this.route.snapshot.paramMap.get('id')));
        this.cdr.detectChanges();
        alertSuccess('Veículo reativado com sucesso.');
      },
      error: (err) => {
        this.loading = false;
        this.cdr.detectChanges();
        alertError(`Erro ao reativar o veículo. ${err?.error?.message || ''}`);
      },
    });
  }
}
