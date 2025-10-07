import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ExpenseService } from '../../../services/expense.service';
import { BaseChartDirective } from 'ng2-charts';
import { ExpenseReport, ExpenseType } from '../../../interfaces/expense';
import {
  FilterConfig,
  BaseFilterComponent,
} from '../../../components/base-filter-component/base-filter-component';
import { ChartData, ChartOptions } from 'chart.js';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-report-expenses',
  templateUrl: './report-expenses.html',
  imports: [BaseFilterComponent, DatePipe, DecimalPipe, BaseChartDirective],
})
export class ReportExpense implements OnInit {
  private expenseService = inject(ExpenseService);
  private cdr = inject(ChangeDetectorRef);

  loadingIndicators = false;
  filter: any = {};
  expenseFilters: FilterConfig[] = [
    { key: 'startDate', label: 'Data de Início', type: 'date' },
    { key: 'endDate', label: 'Data de Fim', type: 'date' },
  ];

  expenseReport: ExpenseReport = {
    distributions: {
      byType: [
        { type: ExpenseType.COMBUSTIVEL, totalAmount: 0, totalCount: 0 },
        { type: ExpenseType.MANUTENCAO, totalAmount: 0, totalCount: 0 },
        { type: ExpenseType.ALIMENTACAO, totalAmount: 0, totalCount: 0 },
        { type: ExpenseType.HOSPEDAGEM, totalAmount: 0, totalCount: 0 },
        { type: ExpenseType.MULTAS, totalAmount: 0, totalCount: 0 },
        { type: ExpenseType.IMPOSTOS, totalAmount: 0, totalCount: 0 },
        { type: ExpenseType.OUTROS, totalAmount: 0, totalCount: 0 },
      ],
      byVehicle: [{ vehiclePlate: '', totalAmount: 0, totalCount: 0 }],
      byDriver: [{ driverName: '', totalAmount: 0, totalCount: 0 }],
      byMonth: [{ month: '', totalAmount: 0 }],
    },
    summary: {
      totalAmount: 0,
      totalCount: 0,
      avgExpenseAmount: 0,
      topExpenseType: { type: ExpenseType.COMBUSTIVEL, totalAmount: 0 },
      topVehicleByAmount: { plate: '', amount: 0 },
      topDriverByAmount: { name: '', amount: 0 },
      lastExpense: { date: '', type: ExpenseType.COMBUSTIVEL, amount: 0 },
    },
  };

  /** Gráficos */
  typeChartData: ChartData<'pie'> = {
    labels: [
      ExpenseType.COMBUSTIVEL,
      ExpenseType.MANUTENCAO,
      ExpenseType.ALIMENTACAO,
      ExpenseType.HOSPEDAGEM,
      ExpenseType.MULTAS,
      ExpenseType.IMPOSTOS,
      ExpenseType.OUTROS,
    ],
    datasets: [
      {
        data: [0, 0, 0, 0, 0, 0, 0],
        backgroundColor: [
          '#34D399',
          '#FBBF24',
          '#3B82F6',
          '#F472B6',
          '#E11D48',
          '#14B8A6',
          '#A1A1AA',
        ],
      },
    ],
  };
  typeChartOptions: ChartOptions<'pie'> = {
    responsive: true,
    plugins: {
      legend: { position: 'right' },
      title: { display: true, text: 'Despesas por Tipo' },
    },
  };

  vehicleChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Valor Total (R$)',
        backgroundColor: '#3B82F6',
      },
    ],
  };
  vehicleChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: {
      legend: { position: 'top' },
      title: { display: true, text: 'Despesas por Veículo' },
    },
  };

  driverChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Valor Total (R$)',
        backgroundColor: '#FBBF24',
      },
    ],
  };
  driverChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: {
      legend: { position: 'top' },
      title: { display: true, text: 'Despesas por Motorista' },
    },
  };

  monthChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Valor Total (R$)',
        borderColor: '#E11D48',
        fill: false,
      },
    ],
  };

  ngOnInit() {
    this.initializeDefaultPeriod();
    this.loadExpenseReport();
  }

  /** Define período padrão (últimos 12 meses) */
  private initializeDefaultPeriod() {
    const now = new Date();
    const start = new Date(now.getFullYear() - 1, now.getMonth(), 1);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);

    this.filter = {
      startDate: start.toISOString().substring(0, 10),
      endDate: end.toISOString().substring(0, 10),
      type: [
        ExpenseType.COMBUSTIVEL,
        ExpenseType.MANUTENCAO,
        ExpenseType.ALIMENTACAO,
        ExpenseType.HOSPEDAGEM,
        ExpenseType.MULTAS,
        ExpenseType.IMPOSTOS,
        ExpenseType.OUTROS,
      ],
      vehiclePlate: '',
      driverName: '',
    };
  }

  /** Atualiza quando filtro mudar */
  applyFilters() {
    this.loadExpenseReport();
  }

  clearFilters() {
    this.initializeDefaultPeriod();
    this.applyFilters();
  }

  /** Chama o backend */
  loadExpenseReport() {
    this.loadingIndicators = true;
    this.expenseService.getReportExpense(this.filter).subscribe({
      next: (res) => {
        console.log(res, 'loadExpenseReport');
        this.expenseReport = res;
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
    // Tipo
    this.typeChartData = {
      labels: this.expenseReport.distributions.byType.map((t) => t.type),
      datasets: [
        {
          data: this.expenseReport.distributions.byType.map(
            (t) => t.totalAmount
          ),
          backgroundColor: [
            '#34D399',
            '#FBBF24',
            '#3B82F6',
            '#F472B6',
            '#E11D48',
            '#14B8A6',
            '#A1A1AA',
          ],
        },
      ],
    };

    // Veículo
    this.vehicleChartData = {
      labels: this.expenseReport.distributions.byVehicle.map(
        (v) => v.vehiclePlate
      ),
      datasets: [
        {
          data: this.expenseReport.distributions.byVehicle.map(
            (v) => v.totalAmount
          ),
          label: 'Valor Total (R$)',
          backgroundColor: '#3B82F6',
        },
      ],
    };

    // Motorista
    this.driverChartData = {
      labels: this.expenseReport.distributions.byDriver.map(
        (d) => d.driverName
      ),
      datasets: [
        {
          data: this.expenseReport.distributions.byDriver.map(
            (d) => d.totalAmount
          ),
          label: 'Valor Total (R$)',
          backgroundColor: '#FBBF24',
        },
      ],
    };

    // Mês
    this.monthChartData = {
      labels: this.expenseReport.distributions.byMonth.map((m) => m.month),
      datasets: [
        {
          data: this.expenseReport.distributions.byMonth.map(
            (m) => m.totalAmount
          ),
          label: 'Valor Total (R$)',
          borderColor: '#E11D48',
          fill: false,
        },
      ],
    };
  }
}
