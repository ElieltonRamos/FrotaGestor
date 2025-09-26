import { Injectable } from '@angular/core';
import { API_URL } from './api.url';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../interfaces/paginator';
import { Trip, TripStatus } from '../interfaces/trip';
import { Message } from '../interfaces/user';

@Injectable({
  providedIn: 'root',
})
export class TripService {
  constructor(private http: HttpClient) {}

  create(trip: Trip): Observable<Message> {
    return this.http.post<Message>(`${API_URL}/trips`, trip);
  }

  getAll(
    page: number = 1,
    limit: number = 10,
    filters: any = {},
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<Trip>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('sortBy', sortKey)
      .set('order', sortAsc ? 'asc' : 'desc');

    // filtros
    if (filters.id) {
      params = params.set('id', filters.id);
    }
    if (filters.vehiclePlate) {
      params = params.set('vehiclePlate', filters.vehiclePlate);
    }
    if (filters.driverName) {
      params = params.set('driverName', filters.driverName);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.startDate) {
      params = params.set('startDate', filters.startDate); // deve ser string ISO
    }
    if (filters.endDate) {
      params = params.set('endDate', filters.endDate); // deve ser string ISO
    }

    const url = `${API_URL}/trips?${params.toString()}`;
    console.log(url)
    return this.http.get<PaginatedResponse<Trip>>(url);
  }

  getById(id: number | string): Observable<Trip> {
    return this.http.get<Trip>(`${API_URL}/trips/${id}`);
  }

  update(id: number | string, trip: Partial<Trip>): Observable<any> {
    return this.http.patch(`${API_URL}/trips/${id}`, trip);
  }

  delete(id: number | string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/trips/${id}`);
  }
}
