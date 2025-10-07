import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { API_URL } from './api.url';
import { Driver, DriverIndicators, DriverReport } from '../interfaces/driver';
import { PaginatedResponse } from '../interfaces/paginator';
import { Message } from '../interfaces/user';

const MOCK_INDICATORS: DriverIndicators = {
  total: 15,
  withExpiredLicense: 2,
  withExpiringLicense: 3,
  mostCommonCategory: 'AB',
  lastDriver: {
    name: 'Carlos Pereira',
    cpf: '123.456.789-00',
    date: '2025-10-05',
  },
};

export const MOCK_DRIVER_REPORT: DriverReport = {
  distributions: {
    totalDrivers: 12,
    cnhExpiringSoon: 2,
    cnhExpired: 1,
    byCategory: [
      { category: 'AB', count: 6 },
      { category: 'C', count: 3 },
      { category: 'D', count: 2 },
      { category: 'E', count: 1 },
    ],
  },
  driversStats: [
    {
      driverId: 1,
      driverName: 'João Silva',
      totalTrips: 25,
      totalDistance: 1200, // km
      totalCost: 1500, // R$
      averageFuelConsumption: 0.12, // L/km
      lastTripDate: '2025-10-05T14:30:00Z',
    },
    {
      driverId: 2,
      driverName: 'Maria Souza',
      totalTrips: 18,
      totalDistance: 900,
      totalCost: 1100,
      averageFuelConsumption: 0.15,
      lastTripDate: '2025-10-04T09:20:00Z',
    },
    {
      driverId: 3,
      driverName: 'Carlos Oliveira',
      totalTrips: 12,
      totalDistance: 600,
      totalCost: 700,
      averageFuelConsumption: 0.14,
      lastTripDate: '2025-10-03T16:45:00Z',
    },
    {
      driverId: 4,
      driverName: 'Ana Lima',
      totalTrips: 30,
      totalDistance: 1500,
      totalCost: 1800,
      averageFuelConsumption: 0.11,
      lastTripDate: '2025-10-05T12:15:00Z',
    },
  ],
};

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

  /** GET Driver Indicators */
  getIndicators(
    filters: Record<string, any> = {}
  ): Observable<DriverIndicators> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });

    // Retornar mock para desenvolvimento
    console.log('🧪 Retornando dados mockados de indicadores de motoristas...');
    return of(MOCK_INDICATORS);

    // Para backend real, descomente abaixo:
    // return this.http.get<DriverIndicators>(`${this.API_URL}/drivers/indicators`, { params });
  }

  /** GET Driver Report */
  getReportDriver(filters: Record<string, any> = {}): Observable<DriverReport> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });

    // Retornar mock para desenvolvimento
    console.log('🧪 Retornando dados mockados de relatório de motoristas...');
    return of(MOCK_DRIVER_REPORT);

    // Para backend real, descomente abaixo:
    // return this.http.get<DriverReport>(`${this.API_URL}/drivers/report`, { params });
  }
}
