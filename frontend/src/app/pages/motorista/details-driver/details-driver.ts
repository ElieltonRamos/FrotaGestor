import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { Driver } from '../../../interfaces/driver';
import { ActivatedRoute, Router } from '@angular/router';
import { DriverService } from '../../../services/driver.service';
import { alertError } from '../../../utils/custom-alerts';

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

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.serviceDriver.getById(id).subscribe({
        next: (res: Driver) => {
          this.driver = res
          this.cdr.detectChanges();
        },
        error: () => alertError('Não foi Possivel buscar as informacoes desse motorista'),
      });
    }
  }

  goBack() {
    this.router.navigate(['/motoristas']);
  }
}
