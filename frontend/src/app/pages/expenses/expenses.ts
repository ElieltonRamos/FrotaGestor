import { Component } from '@angular/core';
import { CreateExpense } from "./create-expense/create-expense";
import { ListExpense } from "./list-expense/list-expense";

@Component({
  selector: 'app-expenses',
  imports: [CreateExpense, ListExpense],
  templateUrl: './expenses.html',
  styles: ``
})
export class Expenses {
  activeTab: 'create' | 'list' = 'list';

  selectTab(tab: 'create' | 'list') {
    this.activeTab = tab;
  }
}
