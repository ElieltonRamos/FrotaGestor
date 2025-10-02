import { Component } from '@angular/core';
import { CreateRefueling } from "./create-refueling/create-refueling";
import { ListRefueling } from "./list-refueling/list-refueling";

@Component({
  selector: 'app-refueling',
  imports: [CreateRefueling, ListRefueling],
  templateUrl: './refueling.html',
  styles: ``
})
export class Refueling {
  activeTab: 'create' | 'list' = 'list';

  selectTab(tab: 'create' | 'list') {
    this.activeTab = tab;
  }
}
