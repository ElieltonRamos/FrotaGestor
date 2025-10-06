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
        { brand: '', count: 0 },
        { brand: '', count: 0 },
        { brand: '', count: 0 },
      ],
      byYear: [
        { year: 0, count: 0 },
        { year: 0, count: 0 },
        { year: 0, count: 0 },
      ],
      byStatus: [
        { status: 'ATIVO', count: 0 },
        { status: 'MANUTENCAO', count: 0 },
      ],
    },
    usageStats: {
      totalDistanceByVehicle: [
        {
          plate: '',
          totalKm: 0,
          totalTrips: 0,
          topDriver: { name: '', trips: 0 },
          fuelCost: 0,
          maintenanceCost: 0,
          totalCost: 0,
          lastMaintenanceDate: '',
          isInUse: false,
        },
      ],
      fuelConsumptionByVehicle: [
        { plate: '', litersPerKm: 0 },
        { plate: '', litersPerKm: 0 },
      ],
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
        data: [0, 0],
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

  ngOnInit() {
    this.initializeDefaultPeriod();
    this.loadIndicators();
    this.loadVehicleReport();
  }

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
        console.log(res, 'loadIndicators');
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
    this.loadingIndicators = true;
    this.vehicleService.getReport(this.filter).subscribe({
      next: (res) => {
        console.log(res, 'loadVehicleReport');
        this.vehicleReport = res;
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
    const v = this.vehicleReport.usageStats.totalDistanceByVehicle;

    // Total de custos
    this.totalCostChartData = {
      labels: v.map((x) => x.plate),
      datasets: [
        {
          label: 'Combustível (R$)',
          data: v.map((x) => x.fuelCost),
          backgroundColor: '#FCA5A5',
        },
        {
          label: 'Manutenção (R$)',
          data: v.map((x) => x.maintenanceCost),
          backgroundColor: '#FB7185',
        },
        {
          label: 'Total (R$)',
          data: v.map((x) => x.totalCost),
          backgroundColor: '#E11D48',
        },
      ],
    };

    // Ano
    this.yearChartData = {
      labels: this.vehicleReport.distributions.byYear.map((y) =>
        y.year.toString()
      ),
      datasets: [
        {
          data: this.vehicleReport.distributions.byYear.map((y) => y.count),
          label: 'Veículos',
          backgroundColor: '#22D3EE',
        },
      ],
    };

    // Marca
    this.brandChartData = {
      labels: this.vehicleReport.distributions.byBrand.map((b) => b.brand),
      datasets: [
        {
          data: this.vehicleReport.distributions.byBrand.map((b) => b.count),
          label: 'Veículos',
          backgroundColor: '#3B82F6',
        },
      ],
    };

    // Status
    this.statusChartData = {
      labels: this.vehicleReport.distributions.byStatus.map((s) => s.status),
      datasets: [
        {
          data: this.vehicleReport.distributions.byStatus.map((s) => s.count),
          backgroundColor: ['#34D399', '#FBBF24', '#F472B6'],
        },
      ],
    };

    // Consumo de combustível
    this.fuelChartData = {
      labels: this.vehicleReport.usageStats.fuelConsumptionByVehicle.map(
        (v) => v.plate
      ),
      datasets: [
        {
          data: this.vehicleReport.usageStats.fuelConsumptionByVehicle.map(
            (v) => v.litersPerKm
          ),
          label: 'L/km',
          borderColor: '#14B8A6',
          fill: false,
        },
      ],
    };
  }
}
