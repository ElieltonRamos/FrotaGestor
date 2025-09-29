import { Injectable } from '@angular/core';
import { API_URL } from './api.url';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Expense } from '../interfaces/expense';
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
    filters: any = {},
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<Expense>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('sortBy', sortKey)
      .set('order', sortAsc ? 'asc' : 'desc');

    if (filters.id) {
      params = params.set('id', filters.id);
    }
    if (filters.description) {
      params = params.set('description', filters.description);
    }
    if (filters.type) {
      params = params.set('type', filters.type);
    }
    if (filters.date) {
      params = params.set('date', filters.date);
    }
    if (filters.amount) {
      params = params.set('amount', filters.amount);
    }
    if (filters.vehicleId) {
      params = params.set('vehicleId', filters.vehicleId);
    }
    if (filters.driverId) {
      params = params.set('driverId', filters.driverId);
    }
    if (filters.tripId) {
      params = params.set('tripId', filters.tripId);
    }
    if (filters.liters) {
      params = params.set('liters', filters.liters);
    }
    if (filters.pricePerLiter) {
      params = params.set('pricePerLiter', filters.pricePerLiter);
    }
    if (filters.odometer) {
      params = params.set('odometer', filters.odometer);
    }

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
}
