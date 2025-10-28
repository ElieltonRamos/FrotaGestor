import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { Trip, TripStatus } from '../../../interfaces/trip';
import { TripService } from '../../../services/trip.service';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

@Component({
  selector: 'app-details-trip',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './details-trip.html',
  styles: ``,
})
export class DetailsTrip {
  private route = inject(ActivatedRoute);
  private serviceTrip = inject(TripService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  trip?: Trip;
  loading = false;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadTrip(id);
    } else {
      this.router.navigate(['/viagens']);
    }
  }

  private loadTrip(id: number) {
    this.loading = true;
    this.serviceTrip.getById(id).subscribe({
      next: (res) => {
        this.trip = res;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/viagens']);
      },
    });
  }

  goBack() {
    this.router.navigate(['/viagens']);
  }

  startTrip() {
    if (!this.trip?.id) return;
    this.loading = true;
    this.serviceTrip
      .update(this.trip.id, { status: TripStatus.EM_ANDAMENTO })
      .subscribe({
        next: () => {
          this.loadTrip(this.trip!.id!);
          this.loading = false;
          this.cdr.detectChanges();
          alertSuccess('Viagem iniciada com sucesso.');
        },
        error: (err) => {
          this.loading = false;
          this.cdr.detectChanges();
          alertError(`Erro ao iniciar a viagem. ${err.error.message || ''}`);
        },
      });
  }

  finishTrip() {
    if (!this.trip?.id) return;
    this.loading = true;
    this.serviceTrip
      .update(this.trip.id, { status: TripStatus.CONCLUIDA })
      .subscribe({
        next: () => {
          this.loadTrip(this.trip!.id!);
          this.loading = false;
          this.cdr.detectChanges();
          alertSuccess('Viagem concluída com sucesso.');
        },
        error: (err) => {
          this.loading = false;
          this.cdr.detectChanges();
          alertError(`Erro ao concluir a viagem. ${err.error.message || ''}`);
        },
      });
  }

  cancelTrip() {
    if (!this.trip?.id) return;
    this.loading = true;
    this.serviceTrip
      .update(this.trip.id, { status: TripStatus.CANCELADA })
      .subscribe({
        next: () => {
          this.loadTrip(this.trip!.id!);
          this.loading = false;
          this.cdr.detectChanges();
          alertSuccess('Viagem cancelada com sucesso.');
        },
        error: (err) => {
          this.loading = false;
          this.cdr.detectChanges();
          alertError(`Erro ao cancelar a viagem. ${err.error.message || ''}`);
        },
      });
  }

  navigateToTrip() {
    if (this.trip?.id) {
      this.router.navigate(['/viagens', this.trip.id]);
    }
  }

  navigateToVehicle() {
    if (this.trip?.vehicleId) {
      this.router.navigate(['/veiculos', this.trip.vehicleId]);
    }
  }

  navigateToDriver() {
    if (this.trip?.driverId) {
      this.router.navigate(['/motoristas', this.trip.driverId]);
    }
  }
}
