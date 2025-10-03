import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.url';
import { Driver, DriverIndicators } from '../interfaces/driver';
import { PaginatedResponse } from '../interfaces/paginator';
import { Message } from '../interfaces/user';

@Injectable({
  providedIn: 'root',
})
export class DriverService {
  constructor(private http: HttpClient) {}

  create(driver: Driver): Observable<Message> {
    return this.http.post<Message>(`${API_URL}/drivers`, driver);
  }

  getAll(
    page: number = 1,
    limit: number = 10,
    filters: Record<string, any> = {},
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<Driver>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('sortBy', sortKey)
      .set('order', sortAsc ? 'asc' : 'desc');

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });

    return this.http.get<PaginatedResponse<Driver>>(`${API_URL}/drivers`, {
      params,
    });
  }

  getById(id: number | string): Observable<Driver> {
    return this.http.get<Driver>(`${API_URL}/drivers/${id}`);
  }

  update(id: number | string, driver: Partial<Driver>): Observable<any> {
    return this.http.patch(`${API_URL}/drivers/${id}`, driver);
  }

  delete(id: number | string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/drivers/${id}`);
  }

  getIndicators(filters: Record<string, any> = {}): Observable<DriverIndicators> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });

    params = params.set('status', 'ATIVO');

    return this.http.get<DriverIndicators>(`${API_URL}/drivers/indicators`, {
      params,
    });
  }
}
