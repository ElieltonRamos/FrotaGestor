import { Component } from '@angular/core';
import { ReportVehicle } from "./report-vehicle/report-vehicle";
import { ReportDriver } from "./report-driver/report-driver";
import { ReportTrip } from "./report-trip/report-trip";

@Component({
  selector: 'app-reports',
  imports: [ReportVehicle, ReportDriver, ReportTrip],
  templateUrl: './reports.html',
  styles: ``,
})
export class Reports {
  activeTab: 'reportVehicles' | 'reportDrivers' | 'reportTrips'= 'reportVehicles';

  selectTab(tab: any) {
    this.activeTab = tab;
  }
}
