import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { Expense, ExpenseType } from '../../../interfaces/expense';
import { ActivatedRoute, Router } from '@angular/router';
import { ExpenseService } from '../../../services/expense.service';
import { alertError } from '../../../utils/custom-alerts';
import { CommonModule, DatePipe, CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-details-fueling',
  imports: [CommonModule, DatePipe, CurrencyPipe],
  templateUrl: './details-refueling.html',
  styles: ``,
})
export class DetailsRefueling {
  private route = inject(ActivatedRoute);
  private serviceExpense = inject(ExpenseService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  fueling?: Expense;
  loading = false;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadFueling(id);
    } else {
      this.router.navigate(['/abastecimentos']);
    }
  }

  private loadFueling(id: number) {
    this.loading = true;
    this.serviceExpense.getById(id).subscribe({
      next: (res) => {
        if (res.type === ExpenseType.COMBUSTIVEL) {
          this.fueling = res;
        } else {
          // caso o ID seja de outro tipo de despesa
          this.router.navigate(['/abastecimentos']);
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/abastecimentos']);
        alertError('Erro ao carregar abastecimento.');
      },
    });
  }

  goBack() {
    this.router.navigate(['/abastecimentos']);
  }
}
