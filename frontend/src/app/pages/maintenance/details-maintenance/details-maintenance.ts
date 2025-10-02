import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { Expense, ExpenseType } from '../../../interfaces/expense';
import { ActivatedRoute, Router } from '@angular/router';
import { ExpenseService } from '../../../services/expense.service';
import { alertError } from '../../../utils/custom-alerts';
import { CommonModule, DatePipe, CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-details-maintenance',
  imports: [CommonModule, DatePipe, CurrencyPipe],
  templateUrl: './details-maintenance.html',
  styles: ``,
})
export class DetailsMaintenance {
  private route = inject(ActivatedRoute);
  private serviceExpense = inject(ExpenseService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  maintenance?: Expense;
  loading = false;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadMaintenance(id);
    } else {
      this.router.navigate(['/manutencoes']);
    }
  }

  private loadMaintenance(id: number) {
    this.loading = true;
    this.serviceExpense.getById(id).subscribe({
      next: (res) => {
        if (res.type === ExpenseType.MANUTENCAO) {
          this.maintenance = res;
        } else {
          // caso o ID seja de outro tipo de despesa
          this.router.navigate(['/manutencoes']);
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/manutencoes']);
        alertError('Erro ao carregar manutenção.');
      },
    });
  }

  goBack() {
    this.router.navigate(['/manutencoes']);
  }
}
