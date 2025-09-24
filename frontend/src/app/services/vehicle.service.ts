import { Injectable } from '@angular/core';
import { API_URL } from './api.url';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Vehicle, VehicleStatus } from '../interfaces/vehicle';
import { Observable } from 'rxjs';
import { Message } from '../interfaces/user';
import { PaginatedResponse } from '../interfaces/paginator';

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
    filters: any = {},
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<Vehicle>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('sortBy', sortKey)
      .set('sortOrder', sortAsc ? 'asc' : 'desc');

    if (filters.id) {
      params = params.set('id', filters.id);
    }
    if (filters.plate) {
      params = params.set('plate', filters.plate);
    }
    if (filters.model) {
      params = params.set('model', filters.model);
    }
    if (filters.brand) {
      params = params.set('brand', filters.brand);
    }
    if (filters.year) {
      params = params.set('year', filters.year);
    }
    if (filters.status && filters.status !== VehicleStatus.INATIVO) {
      params = params.set('status', filters.status);
    }

    const url = `${API_URL}/vehicles?${params.toString()}`;
    return this.http.get<PaginatedResponse<Vehicle>>(url);
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
}
