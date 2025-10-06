import { Component } from '@angular/core';
import { ReportVehicle } from "./report-vehicle/report-vehicle";
import { Reportdriver } from "./report-driver/report-driver";

@Component({
  selector: 'app-reports',
  imports: [ReportVehicle, Reportdriver],
  templateUrl: './reports.html',
  styles: ``,
})
export class Reports {
  activeTab: 'reportVehicles' | 'reportsDrivers' = 'reportVehicles';

  selectTab(tab: any) {
    this.activeTab = tab;
  }
}
