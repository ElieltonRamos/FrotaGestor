import { Component } from '@angular/core';
import { CreateMaintenance } from "./create-maintenance/create-maintenance";
import { ListMaintenance } from "./list-maintenance/list-maintenance";

@Component({
  selector: 'app-maintenance',
  imports: [CreateMaintenance, ListMaintenance],
  templateUrl: './maintenance.html',
  styles: ``
})
export class Maintenance {
  activeTab: 'create' | 'list' = 'list';

  selectTab(tab: 'create' | 'list') {
    this.activeTab = tab;
  }
}
