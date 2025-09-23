import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { Driver } from '../../../interfaces/driver';
import { ActivatedRoute, Router } from '@angular/router';
import { DriverService } from '../../../services/driver.service';
import { alertConfirm, alertError, alertSuccess } from '../../../utils/custom-alerts';

@Component({
  selector: 'app-details-driver',
  imports: [],
  templateUrl: './details-driver.html',
  styles: ``,
})
export class DetailsDriver {
  private route = inject(ActivatedRoute);
  private serviceDriver = inject(DriverService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  driver?: Driver;
  loading = false;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadDriver(id);
    } else {
      this.router.navigate(['/drivers']);
    }
  }

  private loadDriver(id: number) {
    this.loading = true;
    this.serviceDriver.getById(id).subscribe({
      next: (res) => {
        this.driver = res;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/motoristas']);
      },
    });
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
        alertError(`Erro ao reativar o motorista. Tente novamente. ${err.error.message}`);
      },
    });
  }

  activateDriver() {
    if (!this.driver || !this.driver.id) return;

    this.loading = true;

    this.serviceDriver.update(this.driver.id, { status: 'ATIVO' }).subscribe({
      next: () => {
        this.loading = false;
        this.loadDriver(Number(this.route.snapshot.paramMap.get('id')))
        this.cdr.detectChanges();
        alertSuccess('Motorista reativado com sucesso.');
      },
      error: (err) => {
        this.loading = false;
        this.cdr.detectChanges();
        alertError(`Erro ao reativar o motorista. Tente novamente. ${err.error.message}`);
      },
    });
  }
}
