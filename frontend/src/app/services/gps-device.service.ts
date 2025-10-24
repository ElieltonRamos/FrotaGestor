// src/app/services/gps-device.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.url';
import { GpsDevice } from '../interfaces/gpsDevice';
import { Message } from '../interfaces/user';
import { PaginatedResponse } from '../interfaces/paginator';

@Injectable({
  providedIn: 'root'
})
export class GpsDeviceService {
  constructor(private http: HttpClient) {}

  create(device: Partial<GpsDevice>): Observable<Message> {
    return this.http.post<Message>(`${API_URL}/gps-devices`, device);
  }

  update(id: number, device: Partial<GpsDevice>): Observable<Message> {
    return this.http.patch<Message>(`${API_URL}/gps-devices/${id}`, device);
  }

  getAll(
    page: number = 1,
    limit: number = 10,
    filters: Record<string, any> = {},
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<GpsDevice>> {
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

    return this.http.get<PaginatedResponse<GpsDevice>>(`${API_URL}/gps-devices`, { params });
  }

  getById(id: number): Observable<GpsDevice> {
    return this.http.get<GpsDevice>(`${API_URL}/gps-devices/${id}`);
  }
}