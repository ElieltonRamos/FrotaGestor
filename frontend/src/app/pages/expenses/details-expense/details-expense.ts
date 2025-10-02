import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { Expense } from '../../../interfaces/expense';
import { ExpenseService } from '../../../services/expense.service';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

@Component({
  selector: 'app-details-expense',
  imports: [CommonModule, DatePipe],
  templateUrl: './details-expense.html',
  styles: ``,
})
export class DetailsExpense {
  private route = inject(ActivatedRoute);
  private serviceExpense = inject(ExpenseService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  expense?: Expense;
  loading = false;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadExpense(id);
    } else {
      this.router.navigate(['/despesas']);
    }
  }

  private loadExpense(id: number) {
    this.loading = true;
    this.serviceExpense.getById(id).subscribe({
      next: (res) => {
        this.expense = res;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/despesas']);
      },
    });
  }

  goBack() {
    this.router.navigate(['/despesas']);
  }

  deleteExpense() {
    if (!this.expense || !this.expense.id) return;
    this.loading = true;

    this.serviceExpense.delete(this.expense.id).subscribe({
      next: () => {
        this.loading = false;
        alertSuccess('Despesa excluída com sucesso.');
        this.router.navigate(['/despesas']);
      },
      error: (err) => {
        this.loading = false;
        this.cdr.detectChanges();
        alertError(`Erro ao excluir a despesa. Tente novamente. ${err.error.message}`);
      },
    });
  }
}
