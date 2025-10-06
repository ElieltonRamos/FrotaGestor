import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.url';
import {
  Vehicle,
  VehicleIndicators,
  VehicleReport,
} from '../interfaces/vehicle';
import { PaginatedResponse } from '../interfaces/paginator';
import { Message } from '../interfaces/user';

@Injectable({
  providedIn: 'root',
})
export class VehicleService {
  constructor(private http: HttpClient) {}

  create(vehicle: Vehicle): Observable<Message> {
    return this.http.post<Message>(`${API_URL}/vehicles`, vehicle);
  }

  getAll(
    page: number = 1,
    limit: number = 10,
    filters: Record<string, any> = {},
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<Vehicle>> {
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

    return this.http.get<PaginatedResponse<Vehicle>>(`${API_URL}/vehicles`, {
      params,
    });
  }

  getById(id: number | string): Observable<Vehicle> {
    return this.http.get<Vehicle>(`${API_URL}/vehicles/${id}`);
  }

  update(id: number | string, vehicle: Partial<Vehicle>): Observable<any> {
    return this.http.patch(`${API_URL}/vehicles/${id}`, vehicle);
  }

  delete(id: number | string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/vehicles/${id}`);
  }

  getIndicators(
    filters: Record<string, any> = {}
  ): Observable<VehicleIndicators> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });
    return this.http.get<VehicleIndicators>(`${API_URL}/vehicles/indicators`, {
      params,
    });
  }

  getReport(filters: Record<string, any> = {}): Observable<VehicleReport> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });

    return this.http.get<VehicleReport>(`${API_URL}/vehicles/report`, {
      params,
    });
  }
}
