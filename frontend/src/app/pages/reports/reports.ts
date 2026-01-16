import { Component } from '@angular/core';
import { ReportVehicle } from './report-vehicle/report-vehicle';
import { ReportDriver } from './report-driver/report-driver';
import { ReportTrip } from './report-trip/report-trip';
import { ReportExpense } from './report-expenses/report-expenses';
import { ReportSubfleetsComponent } from "./report-subfleet/report-subfleets";

@Component({
  selector: 'app-reports',
  imports: [ReportVehicle, ReportDriver, ReportTrip, ReportExpense, ReportSubfleetsComponent],
  templateUrl: './reports.html',
  styles: ``,
})
export class Reports {
  activeTab:
    | 'reportVehicles'
    | 'reportDrivers'
    | 'reportTrips'
    | 'reportSubfleets'
    | 'reportExpenses' = 'reportVehicles';

  selectTab(tab: any) {
    this.activeTab = tab;
  }
}
