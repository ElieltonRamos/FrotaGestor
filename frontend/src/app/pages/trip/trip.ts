import { Component } from '@angular/core';
import { CreateTrip } from "./create-trip/create-trip";
import { ListTrip } from "./list-trip/list-trip";

@Component({
  selector: 'app-trip',
  imports: [CreateTrip, ListTrip],
  templateUrl: './trip.html',
  styles: ``
})
export class Trip {
  activeTab: 'create' | 'list' = 'list';

  selectTab(tab: 'create' | 'list') {
    this.activeTab = tab;
  }
}
