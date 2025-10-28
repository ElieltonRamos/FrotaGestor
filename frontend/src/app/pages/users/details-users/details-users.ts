import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import User, { UserRole } from '../../../interfaces/user';
import { UserService } from '../../../services/user.service';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

@Component({
  selector: 'app-details-user',
  imports: [CommonModule],
  templateUrl: './details-users.html',
  styles: ``,
})
export class DetailsUsers {
  private route = inject(ActivatedRoute);
  private serviceUser = inject(UserService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  user?: User;
  loading = false;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadUser(id);
    } else {
      this.router.navigate(['/usuarios']);
    }
  }

  private loadUser(id: number) {
    this.loading = true;
    this.serviceUser.getById(id).subscribe({
      next: (res) => {
        this.user = res;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/usuarios']);
      },
    });
  }

  goBack() {
    this.router.navigate(['/usuarios']);
  }
}
