import { Injectable } from '@angular/core';
import { API_URL } from './api.url';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import {
  Expense,
  ExpenseIndicators,
  ExpenseReport,
  ExpenseType,
  MaintenanceIndicators,
  RefuelingIndicators,
} from '../interfaces/expense';
import { Message } from '../interfaces/user';
import { PaginatedResponse } from '../interfaces/paginator';

export const MOCK_EXPENSE_REPORT: ExpenseReport = {
  distributions: {
    byType: [
      { type: ExpenseType.COMBUSTIVEL, totalAmount: 12500, totalCount: 45 },
      { type: ExpenseType.MANUTENCAO, totalAmount: 8200, totalCount: 20 },
      { type: ExpenseType.ALIMENTACAO, totalAmount: 1500, totalCount: 30 },
      { type: ExpenseType.HOSPEDAGEM, totalAmount: 2300, totalCount: 10 },
    ],
    byVehicle: [
      { vehiclePlate: 'ABC-1234', totalAmount: 9000, totalCount: 25 },
      { vehiclePlate: 'XYZ-9876', totalAmount: 7500, totalCount: 20 },
      { vehiclePlate: 'DEF-5678', totalAmount: 5000, totalCount: 15 },
    ],
    byDriver: [
      { driverName: 'João Silva', totalAmount: 6800, totalCount: 18 },
      { driverName: 'Maria Souza', totalAmount: 5600, totalCount: 15 },
      { driverName: 'Carlos Lima', totalAmount: 4400, totalCount: 12 },
    ],
    byMonth: [
      { month: '2025-07', totalAmount: 6200 },
      { month: '2025-08', totalAmount: 7800 },
      { month: '2025-09', totalAmount: 10500 },
      { month: '2025-10', totalAmount: 9000 },
    ],
  },
  summary: {
    totalAmount: 24500,
    totalCount: 105,
    avgExpenseAmount: 233.33,
    topExpenseType: { type: ExpenseType.COMBUSTIVEL, totalAmount: 12500 },
    topVehicleByAmount: { plate: 'ABC-1234', amount: 9000 },
    topDriverByAmount: { name: 'João Silva', amount: 6800 },
    lastExpense: {
      date: '2025-10-04',
      type: ExpenseType.MANUTENCAO,
      amount: 850,
    },
  },
};

@Injectable({
  providedIn: 'root',
})
export class ExpenseService {
  constructor(private http: HttpClient) {}

  create(expense: Expense): Observable<Message> {
    return this.http.post<Message>(`${API_URL}/expenses`, expense);
  }

  getAll(
    page: number = 1,
    limit: number = 10,
    filters: Record<string, any> = {},
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<Expense>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('sortBy', sortKey)
      .set('order', sortAsc ? 'asc' : 'desc');

    // aplica dinamicamente todos os filtros preenchidos
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });

    const url = `${API_URL}/expenses?${params.toString()}`;
    console.log('🔎 URL chamada:', url);

    return this.http.get<PaginatedResponse<Expense>>(url);
  }

  getById(id: number | string): Observable<Expense> {
    return this.http.get<Expense>(`${API_URL}/expenses/${id}`);
  }

  update(id: number | string, expense: Partial<Expense>): Observable<any> {
    return this.http.patch(`${API_URL}/expenses/${id}`, expense);
  }

  delete(id: number | string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/expenses/${id}`);
  }

  getIndicatorsRefueling(
    filters: Record<string, any> = {}
  ): Observable<RefuelingIndicators> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });
    const url = `${API_URL}/expenses/fuels/indicators?${params.toString()}`;
    return this.http.get<RefuelingIndicators>(url);
  }

  getIndicatorsMaintenance(
    filters: Record<string, any>
  ): Observable<MaintenanceIndicators> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });
    return this.http.get<MaintenanceIndicators>(
      `${API_URL}/expenses/maintenance/indicators`,
      { params }
    );
  }

  getIndicatorsExpenses(
    filters: Record<string, any> = {}
  ): Observable<ExpenseIndicators> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });

    const url = `${API_URL}/expenses/indicators?${params.toString()}`;
    return this.http.get<ExpenseIndicators>(url);
  }

  getReportExpense(filters = {}): Observable<ExpenseReport> {
    // Implementação real: montar query params
    // let params = new HttpParams();
    // Object.entries(filters).forEach(([key, value]) => {
    //   if (value !== undefined && value !== null && value !== '') {
    //     params = params.set(key, value);
    //   }
    // });

    // return this.http.get<ExpenseReport>(`${API_URL}/expenses/report`, {
    //   params,
    // });

    console.log('🧪 Retornando dados mockados de relatório de despesas...');
    return of(MOCK_EXPENSE_REPORT);
  }
}
