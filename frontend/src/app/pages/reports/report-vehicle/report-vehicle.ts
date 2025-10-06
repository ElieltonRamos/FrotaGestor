import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { VehicleService } from '../../../services/vehicle.service';
import { BaseChartDirective } from 'ng2-charts';
import { VehicleIndicators, VehicleReport } from '../../../interfaces/vehicle';
import {
  FilterConfig,
  BaseFilterComponent,
} from '../../../components/base-filter-component/base-filter-component';
import { ChartData, ChartOptions } from 'chart.js';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-report-vehicle',
  templateUrl: './report-vehicle.html',
  imports: [BaseFilterComponent, DatePipe, BaseChartDirective],
})
export class ReportVehicle {
  private vehicleService = inject(VehicleService);
  private cdr = inject(ChangeDetectorRef);

  /** Estado e filtros */
  loadingIndicators = false;
  filter: any = {};
  vehicleFilters: FilterConfig[] = [
    { key: 'startDate', label: 'Data de Início', type: 'date' },
    { key: 'endDate', label: 'Data de Fim', type: 'date' },
  ];

  indicators: VehicleIndicators = {
    active: 0,
    maintenance: 0,
    lastVehicle: { plate: '', date: '' },
  };

  vehicleReport: VehicleReport = {
    distributions: {
      byBrand: [
        { brand: 'Toyota', count: 5 },
        { brand: 'Ford', count: 4 },
        { brand: 'Honda', count: 3 },
      ],
      byYear: [
        { year: 2022, count: 4 },
        { year: 2021, count: 5 },
        { year: 2020, count: 3 },
      ],
      byStatus: [
        { status: 'ATIVO', count: 12 },
        { status: 'MANUTENCAO', count: 3 },
      ],
    },
    usageStats: {
      totalDistanceByVehicle: [
        {
          plate: 'ABC-1234',
          totalKm: 12000,
          totalTrips: 45,
          topDriver: { name: 'João Silva', trips: 20 },
          fuelCost: 4500,
          maintenanceCost: 1200,
          totalCost: 5700,
          lastMaintenanceDate: '2025-09-20',
          isInUse: true,
        },
        {
          plate: 'DEF-5678',
          totalKm: 8000,
          totalTrips: 30,
          topDriver: { name: 'Maria Souza', trips: 15 },
          fuelCost: 3000,
          maintenanceCost: 900,
          totalCost: 3900,
          lastMaintenanceDate: '2025-08-10',
          isInUse: false,
        },
      ],
      fuelConsumptionByVehicle: [
        { plate: 'ABC-1234', litersPerKm: 0.12 },
        { plate: 'DEF-5678', litersPerKm: 0.15 },
      ],
    },
    filters: {
      brands: ['Toyota', 'Ford', 'Honda'],
      models: ['Corolla', 'Focus', 'Civic'],
      years: [2020, 2021, 2022],
      status: ['ATIVO', 'MANUTENCAO'],
    },
  };

  /** Gráficos */
  brandChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [{ data: [], label: 'Veículos' }],
  };
  brandChartOptions: ChartOptions<'bar'> = { responsive: true };

  statusChartData: ChartData<'pie'> = {
    labels: ['ATIVO', 'MANUTENCAO'],
    datasets: [
      {
        data: [12, 3],
        backgroundColor: ['#34D399', '#FBBF24'],
      },
    ],
  };

  statusChartOptions: ChartOptions<'pie'> = { responsive: true };

  fuelChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      { data: [], label: 'L/km', borderColor: '#14B8A6', fill: false },
    ],
  };
  fuelChartOptions: ChartOptions<'line'> = { responsive: true };

  totalCostChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      { label: 'Combustível (R$)', data: [], backgroundColor: '#FCA5A5' },
      { label: 'Manutenção (R$)', data: [], backgroundColor: '#FB7185' },
      { label: 'Total (R$)', data: [], backgroundColor: '#E11D48' },
    ],
  };

  totalCostChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    indexAxis: 'y',
    plugins: {
      legend: { position: 'bottom' },
      title: { display: true, text: 'Custos Totais por Veículo' },
    },
  };

  // Gráfico de Veículos por Ano
  yearChartData: ChartData<'bar'> = {
    labels: [], // anos
    datasets: [
      {
        data: [],
        label: 'Veículos',
        backgroundColor: '#22D3EE',
      },
    ],
  };

  yearChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: {
      legend: { position: 'top' },
      title: { display: true, text: 'Veículos por Ano' },
    },
  };

  // ngOnInit() {
  //   this.initializeDefaultPeriod();
  //   this.loadIndicators();
  //   this.loadVehicleReport();
  // }

  /** Define período padrão (mês atual) */
  private initializeDefaultPeriod() {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);

    this.filter = {
      startDate: start.toISOString().substring(0, 10),
      endDate: end.toISOString().substring(0, 10),
    };
  }

  /** Atualiza quando filtro mudar */
  applyFilters() {
    this.loadIndicators();
    this.loadVehicleReport();
  }

  clearFilters() {
    this.initializeDefaultPeriod();
    this.applyFilters();
  }

  /** Chama o backend */
  loadIndicators() {
    this.loadingIndicators = true;
    this.vehicleService.getIndicators(this.filter).subscribe({
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

  loadVehicleReport() {
    this.vehicleService.getReport(this.filter).subscribe({
      next: (res) => {
        this.vehicleReport = res;
        this.updateCharts();
        this.cdr.detectChanges();
      },
    });
  }

  /** Atualiza gráficos */
  private updateCharts() {
    const v = this.vehicleReport.usageStats.totalDistanceByVehicle;

    this.totalCostChartData.labels = v.map((x) => x.plate);
    this.totalCostChartData.datasets[0].data = v.map((x) => x.fuelCost);
    this.totalCostChartData.datasets[1].data = v.map((x) => x.maintenanceCost);
    this.totalCostChartData.datasets[2].data = v.map((x) => x.totalCost);
    this.yearChartData.labels = this.vehicleReport.distributions.byYear.map(
      (y) => y.year.toString()
    );
    this.yearChartData.datasets[0].data =
      this.vehicleReport.distributions.byYear.map((y) => y.count);

    this.brandChartData.labels = this.vehicleReport.distributions.byBrand.map(
      (b) => b.brand
    );
    this.brandChartData.datasets[0].data =
      this.vehicleReport.distributions.byBrand.map((b) => b.count);

    this.statusChartData.labels = this.vehicleReport.distributions.byStatus.map(
      (s) => s.status
    );
    this.statusChartData.datasets[0].data =
      this.vehicleReport.distributions.byStatus.map((s) => s.count);
    this.statusChartData.datasets[0].backgroundColor = [
      '#34D399',
      '#FBBF24',
      '#F472B6',
    ];

    this.fuelChartData.labels =
      this.vehicleReport.usageStats.fuelConsumptionByVehicle.map(
        (v) => v.plate
      );
    this.fuelChartData.datasets[0].data =
      this.vehicleReport.usageStats.fuelConsumptionByVehicle.map(
        (v) => v.litersPerKm
      );
  }
}
