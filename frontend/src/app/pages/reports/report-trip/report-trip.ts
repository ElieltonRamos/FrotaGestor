import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { TripService } from '../../../services/trip.service';
import { BaseChartDirective } from 'ng2-charts';
import {
  TripIndicators,
  TripReport,
  TripStatus,
} from '../../../interfaces/trip';
import {
  FilterConfig,
  BaseFilterComponent,
} from '../../../components/base-filter-component/base-filter-component';
import { ChartData, ChartOptions } from 'chart.js';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-report-trip',
  templateUrl: './report-trip.html',
  imports: [BaseFilterComponent, DatePipe, DecimalPipe, BaseChartDirective],
})
export class ReportTrip implements OnInit {
  private tripService = inject(TripService);
  private cdr = inject(ChangeDetectorRef);

  loadingIndicators = false;
  filter: any = {};
  tripFilters: FilterConfig[] = [
    { key: 'startDate', label: 'Data de Início', type: 'date' },
    { key: 'endDate', label: 'Data de Fim', type: 'date' },
  ];

  indicators: TripIndicators = {
    totalTrips: 0,
    inProgress: 0,
    completed: 0,
    canceled: 0,
    totalDistance: 0,
    avgDistance: 0,
    lastTrip: { date: '', driverName: '', vehiclePlate: '' },
  };

  tripReport: TripReport = {
    distributions: {
      byStatus: [
        { status: TripStatus.PLANEJADA, count: 0 },
        { status: TripStatus.EM_ANDAMENTO, count: 0 },
        { status: TripStatus.CONCLUIDA, count: 0 },
        { status: TripStatus.CANCELADA, count: 0 },
      ],
      byVehicle: [{ vehiclePlate: '', count: 0, totalCost: 0 }],
      byDriver: [{ driverName: '', count: 0, totalCost: 0 }],
      byDestination: [{ destination: '', totalTrips: 0, totalCost: 0 }],
    },
  };

  /** Gráficos */
  statusChartData: ChartData<'pie'> = {
    labels: [
      TripStatus.PLANEJADA,
      TripStatus.EM_ANDAMENTO,
      TripStatus.CONCLUIDA,
      TripStatus.CANCELADA,
    ],
    datasets: [
      {
        data: [0, 0, 0, 0],
        backgroundColor: ['#34D399', '#FBBF24', '#3B82F6', '#F472B6'],
      },
    ],
  };
  statusChartOptions: ChartOptions<'pie'> = {
    responsive: true,
    plugins: {
      legend: { position: 'right' },
      title: { display: true, text: 'Viagens por Status' },
    },
  };

  vehicleChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Viagens',
        backgroundColor: '#3B82F6',
      },
    ],
  };
  vehicleChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: {
      legend: { position: 'top' },
      title: { display: true, text: 'Viagens por Veículo' },
    },
  };

  driverChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Viagens',
        backgroundColor: '#FBBF24',
      },
    ],
  };
  driverChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: {
      legend: { position: 'top' },
      title: { display: true, text: 'Viagens por Motorista' },
    },
  };

  destinationChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Viagens',
        backgroundColor: '#E11D48',
      },
    ],
  };
  destinationChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: {
      legend: { position: 'top' },
      title: { display: true, text: 'Viagens por Destino' },
    },
  };

  destinationCostChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Custo Total (R$)',
        backgroundColor: '#14B8A6',
      },
    ],
  };
  destinationCostChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: {
      legend: { position: 'top' },
      title: { display: true, text: 'Custo Total por Destino' },
    },
  };

  ngOnInit() {
    this.initializeDefaultPeriod();
    this.loadIndicators();
    this.loadTripReport();
  }

  /** Define período padrão (mês atual) */
  private initializeDefaultPeriod() {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);

    this.filter = {
      startDate: start.toISOString().substring(0, 10),
      endDate: end.toISOString().substring(0, 10),
      status: [
        TripStatus.PLANEJADA,
        TripStatus.EM_ANDAMENTO,
        TripStatus.CONCLUIDA,
        TripStatus.CANCELADA,
      ],
      destination: '',
    };
  }

  /** Atualiza quando filtro mudar */
  applyFilters() {
    this.loadIndicators();
    this.loadTripReport();
  }

  clearFilters() {
    this.initializeDefaultPeriod();
    this.applyFilters();
  }

  /** Chama o backend */
  loadIndicators() {
    this.loadingIndicators = true;
    this.tripService.getIndicators(this.filter).subscribe({
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

  loadTripReport() {
    this.loadingIndicators = true;
    this.tripService.getReport(this.filter).subscribe({
      next: (res) => {
        this.tripReport = res;
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
    // Status
    this.statusChartData = {
      labels: this.tripReport.distributions.byStatus.map((s) => s.status),
      datasets: [
        {
          data: this.tripReport.distributions.byStatus.map((s) => s.count),
          backgroundColor: ['#34D399', '#FBBF24', '#3B82F6', '#F472B6'],
        },
      ],
    };

    // Veículo
    this.vehicleChartData = {
      labels: this.tripReport.distributions.byVehicle.map(
        (v) => v.vehiclePlate
      ),
      datasets: [
        {
          data: this.tripReport.distributions.byVehicle.map((v) => v.count),
          label: 'Viagens',
          backgroundColor: '#3B82F6',
        },
      ],
    };

    // Motorista
    this.driverChartData = {
      labels: this.tripReport.distributions.byDriver.map((d) => d.driverName),
      datasets: [
        {
          data: this.tripReport.distributions.byDriver.map((d) => d.count),
          label: 'Viagens',
          backgroundColor: '#FBBF24',
        },
      ],
    };

    // Destino (Viagens)
    this.destinationChartData = {
      labels: this.tripReport.distributions.byDestination.map(
        (d) => d.destination
      ),
      datasets: [
        {
          data: this.tripReport.distributions.byDestination.map(
            (d) => d.totalTrips
          ),
          label: 'Viagens',
          backgroundColor: '#E11D48',
        },
      ],
    };

    // Custo por Destino
    this.destinationCostChartData = {
      labels: this.tripReport.distributions.byDestination.map(
        (d) => d.destination
      ),
      datasets: [
        {
          data: this.tripReport.distributions.byDestination.map(
            (d) => d.totalCost
          ),
          label: 'Custo Total (R$)',
          backgroundColor: '#14B8A6',
        },
      ],
    };
  }
}
