import { Injectable } from '@angular/core';
import { API_URL } from './api.url';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { PaginatedResponse } from '../interfaces/paginator';
import {
  Trip,
  TripIndicators,
  TripReport,
} from '../interfaces/trip';
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
    filters: Record<string, any> = {},
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<Trip>> {
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

    const url = `${API_URL}/trips?${params.toString()}`;
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

  getIndicators(filters: Record<string, any> = {}): Observable<TripIndicators> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });

    return this.http.get<TripIndicators>(`${API_URL}/trips/indicators`, {
      params,
    });
  }

  getReport(filters: Record<string, any> = {}): Observable<TripReport> {
    let params = new HttpParams();

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });

    return this.http.get<TripReport>(`${API_URL}/reports/trips`, {
      params,
    });
  }
}
