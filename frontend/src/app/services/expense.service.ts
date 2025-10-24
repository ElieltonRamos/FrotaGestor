import { Injectable } from '@angular/core';
import { API_URL } from './api.url';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import {
  Expense,
  ExpenseIndicators,
  ExpenseReport,
  MaintenanceIndicators,
  RefuelingIndicators,
} from '../interfaces/expense';
import { Message } from '../interfaces/user';
import { PaginatedResponse } from '../interfaces/paginator';

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
    const url = `${API_URL}/expenses/refueling-indicators?${params.toString()}`;
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
      `${API_URL}/expenses/maintenance-indicators`,
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
    const url = `${API_URL}/expenses/expense-indicators?${params.toString()}`;
    return this.http.get<ExpenseIndicators>(url);
  }

  getReportExpense(filters: Record<string, any> = {}): Observable<ExpenseReport> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });
    return this.http.get<ExpenseReport>(`${API_URL}/report/expenses`, {
      params,
    });
  }
}
