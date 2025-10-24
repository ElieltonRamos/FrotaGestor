import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DriverService } from '../../../services/driver.service';
import { BaseChartDirective } from 'ng2-charts';
import { DriverIndicators, DriverReport } from '../../../interfaces/driver';
import {
  FilterConfig,
  BaseFilterComponent,
} from '../../../components/base-filter-component/base-filter-component';
import { ChartData, ChartOptions } from 'chart.js';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-report-driver',
  templateUrl: './report-driver.html',
  imports: [BaseFilterComponent, DatePipe, BaseChartDirective],
})
export class ReportDriver implements OnInit {
  private driverService = inject(DriverService);
  private cdr = inject(ChangeDetectorRef);

  loadingIndicators = false;
  filter: any = {};
  driverFilters: FilterConfig[] = [
    { key: 'startDate', label: 'Data de Início', type: 'date' },
    { key: 'endDate', label: 'Data de Fim', type: 'date' },
  ];

  indicators: DriverIndicators = {
    total: 0,
    withExpiredLicense: 0,
    withExpiringLicense: 0,
    mostCommonCategory: '',
    lastDriver: { name: '', cpf: '', date: '' },
  };

  driverReport: DriverReport = {
    distributions: {
      totalDrivers: 0,
      cnhExpiringSoon: 0,
      cnhExpired: 0,
      byCategory: [
        { category: 'A', count: 0 },
        { category: 'B', count: 0 },
        { category: 'C', count: 0 },
      ],
    },
    driversStats: [
      {
        driverName: '',
        driverId: 0,
        totalTrips: 0,
        totalDistance: 0,
        totalCost: 0,
        averageFuelConsumption: 0,
        lastTripDate: '',
      },
    ],
  };

  /** Gráficos */
  categoryChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Motoristas',
        backgroundColor: '#3B82F6',
      },
    ],
  };
  categoryChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: {
      legend: { position: 'top' },
      title: { display: true, text: 'Motoristas por Categoria CNH' },
    },
  };

  licenseValidityChartData: ChartData<'pie'> = {
    labels: ['Válida', 'Vencendo em 30d', 'Expirada'],
    datasets: [
      {
        data: [0, 0, 0],
        backgroundColor: ['#34D399', '#FBBF24', '#F472B6'],
      },
    ],
  };
  licenseValidityChartOptions: ChartOptions<'pie'> = {
    responsive: true,
    plugins: {
      legend: { position: 'right' },
      title: { display: true, text: 'Motoristas por Validade da CNH' },
    },
  };

  totalCostChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        label: 'Custo Total (R$)',
        data: [],
        backgroundColor: '#E11D48',
      },
    ],
  };
  totalCostChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    indexAxis: 'y',
    plugins: {
      legend: { position: 'bottom' },
      title: { display: true, text: 'Custos Totais por Motorista' },
    },
  };

  fuelConsumptionChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        label: 'Consumo Médio (L/km)',
        data: [],
        borderColor: '#14B8A6',
        fill: false,
      },
    ],
  };
  fuelConsumptionChartOptions: ChartOptions<'line'> = {
    responsive: true,
    plugins: {
      legend: { position: 'top' },
      title: {
        display: true,
        text: 'Consumo Médio de Combustível por Motorista',
      },
    },
  };

  ngOnInit() {
    this.initializeDefaultPeriod();
    this.loadIndicators();
    this.loadDriverReport();
  }

  /** Define período padrão (mês atual) */
  private initializeDefaultPeriod() {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);

    this.filter = {
      startDate: start.toISOString().substring(0, 10),
      endDate: end.toISOString().substring(0, 10),
      categories: ['A', 'B', 'C', 'D', 'E'],
    };
  }

  /** Atualiza quando filtro mudar */
  applyFilters() {
    this.loadIndicators();
    this.loadDriverReport();
  }

  clearFilters() {
    this.initializeDefaultPeriod();
    this.applyFilters();
  }

  /** Chama o backend */
  loadIndicators() {
    this.loadingIndicators = true;
    this.driverService.getIndicators(this.filter).subscribe({
      next: (res) => {
        this.indicators = res;
        this.loadingIndicators = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingIndicators = false;
      },
    });
  }

  loadDriverReport() {
    this.loadingIndicators = true;
    this.driverService.getReportDriver(this.filter).subscribe({
      next: (res) => {
        this.driverReport = res;
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
    // Categoria CNH
    this.categoryChartData = {
      labels: this.driverReport.distributions.byCategory.map((c) => c.category),
      datasets: [
        {
          data: this.driverReport.distributions.byCategory.map((c) => c.count),
          label: 'Motoristas',
          backgroundColor: '#3B82F6',
        },
      ],
    };

    // Validade da CNH
    this.licenseValidityChartData = {
      labels: ['Válida', 'Vencendo em Breve', 'Expirada'],
      datasets: [
        {
          data: [
            this.driverReport.distributions.totalDrivers -
              (this.driverReport.distributions.cnhExpiringSoon +
                this.driverReport.distributions.cnhExpired),
            this.driverReport.distributions.cnhExpiringSoon,
            this.driverReport.distributions.cnhExpired,
          ],
          backgroundColor: ['#34D399', '#FBBF24', '#F472B6'],
        },
      ],
    };

    // Custo Total por Motorista
    this.totalCostChartData = {
      labels: this.driverReport.driversStats.map((d) => d.driverName),
      datasets: [
        {
          label: 'Custo Total (R$)',
          data: this.driverReport.driversStats.map((d) => d.totalCost),
          backgroundColor: '#E11D48',
        },
      ],
    };

    // Consumo Médio de Combustível
    this.fuelConsumptionChartData = {
      labels: this.driverReport.driversStats.map((d) => d.driverName),
      datasets: [
        {
          label: 'Consumo Médio (L/km)',
          data: this.driverReport.driversStats.map(
            (d) => d.averageFuelConsumption || 0
          ),
          borderColor: '#14B8A6',
          fill: false,
        },
      ],
    };
  }
}
