import { Component } from '@angular/core';
import { ListVehicle } from "./list-vehicle/list-vehicle";
import { CreateVehicle } from './create-vehicle/create-vehicle';

@Component({
  selector: 'app-vehicles',
  imports: [CreateVehicle, ListVehicle],
  templateUrl: './vehicles.html',
  styles: ``
})
export class Vehicles {
  activeTab: 'create' | 'list' = 'list';

  selectTab(tab: 'create' | 'list') {
    this.activeTab = tab;
  }
}
