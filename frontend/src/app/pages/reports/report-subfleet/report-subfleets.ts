// report-subfleets.component.ts
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import {
  FilterConfig,
  BaseFilterComponent,
} from '../../../components/base-filter-component/base-filter-component';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { SubfleetService } from '../../../services/subfleet.service';
import { SubfleetReportResponse } from '../../../interfaces/subfleet';
import { ChartData, ChartOptions } from 'chart.js';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-report-subfleets',
  templateUrl: './report-subfleets.html',
  imports: [BaseFilterComponent, DecimalPipe, BaseChartDirective, CommonModule],
  standalone: true,
})
export class ReportSubfleetsComponent implements OnInit {
  private subfleetReportService = inject(SubfleetService);
  private cdr = inject(ChangeDetectorRef);

  loadingIndicators = false;
  filter: any = {};
  subfleetFilters: FilterConfig[] = [
    { key: 'startDate', label: 'Data de Início', type: 'date' },
    { key: 'endDate', label: 'Data de Fim', type: 'date' },
  ];

  report: SubfleetReportResponse | null = null;

  /** Gráficos */
  rankingChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [{ data: [], label: 'R$/km', backgroundColor: [] }],
  };
  rankingChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    indexAxis: 'y', // Barras horizontais
    plugins: {
      legend: { display: false },
      title: { display: true, text: 'Custo por Km (Ranking)' },
    },
    scales: {
      x: { 
        title: { display: true, text: 'R$/km' },
        ticks: { callback: (value) => `R$ ${value}` }
      }
    }
  };

  ngOnInit() {
    this.initializeDefaultPeriod();
    this.loadSubfleetReport();
  }

  private initializeDefaultPeriod() {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    this.filter = {
      startDate: start.toISOString().substring(0, 10),
      endDate: end.toISOString().substring(0, 10),
    };
  }

  applyFilters() {
    this.loadSubfleetReport();
  }

  clearFilters() {
    this.initializeDefaultPeriod();
    this.applyFilters();
  }

  loadSubfleetReport() {
    this.loadingIndicators = true;
    const period = {
      startDate: this.filter.startDate,
      endDate: this.filter.endDate
    };
    
    this.subfleetReportService.getSubfleetReport(period).subscribe({
      next: (res) => {
        this.report = res;
        this.updateCharts();
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingIndicators = false;
      },
    });
  }

  private updateCharts() {
    if (!this.report?.subfleets.length) return;

    // Ordena por costPerKm (melhor primeiro)
    const sortedSubfleets = [...this.report.subfleets]
      .sort((a, b) => a.costPerKm - b.costPerKm)

    this.rankingChartData = {
      labels: sortedSubfleets.map(s => s.name),
      datasets: [{
        data: sortedSubfleets.map(s => s.costPerKm),
        label: 'R$/km',
        backgroundColor: sortedSubfleets.map(s => {
          if (s.costPerKm < 0.4) return '#10B981';      // 🟢 Verde
          if (s.costPerKm < 0.55) return '#F59E0B';      // 🟡 Amarelo
          return '#EF4444';                              // 🔴 Vermelho
        })
      }]
    };
  }

  // Getters para template
  get summary() {
    return this.report?.summary || { totalSubfleets: 0, totalVehicles: 0, overallEfficiency: 0 };
  }

  get subfleets() {
    return this.report?.subfleets || [];
  }

  get sortedSubfleetsByDistance() {
    return this.subfleets.slice()
      .sort((a, b) => b.totalDistanceKm - a.totalDistanceKm)
      .slice(0, 3);
  }

  get sortedSubfleetsByMaintenance() {
    return this.subfleets.slice()
      .sort((a, b) => b.maintenanceVehicles - a.maintenanceVehicles)
      .slice(0, 3);
  }

  get sortedSubfleetsByEfficiency() {
    return this.subfleets.slice()
      .sort((a, b) => b.vehiclesActiveRate - a.vehiclesActiveRate)
      .slice(0, 3);
  }
}
