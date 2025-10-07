import { Injectable } from '@angular/core';
import { API_URL } from './api.url';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { PaginatedResponse } from '../interfaces/paginator';
import {
  Trip,
  TripIndicators,
  TripReport,
  TripStatus,
} from '../interfaces/trip';
import { Message } from '../interfaces/user';

const mockTripIndicators: TripIndicators = {
  totalTrips: 128,
  inProgress: 7,
  completed: 103,
  canceled: 18,
  totalDistance: 45280, // km total
  avgDistance: 354, // km médio por viagem
  lastTrip: {
    date: '2025-10-05T16:20:00Z',
    driverName: 'Carlos Andrade',
    vehiclePlate: 'ABC-9F45',
  },
};

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
    console.log('🔎 URL chamada:', url);

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
    // let params = new HttpParams();
    // Object.entries(filters).forEach(([key, value]) => {
    //   if (value !== undefined && value !== null && value !== '') {
    //     params = params.set(key, value);
    //   }
    // });

    // return this.http.get<TripIndicators>(`${API_URL}/trips/indicators`, {
    //   params,
    // });
    console.log('🧪 Retornando dados mockados de indicadores de viagens...');
    return of(mockTripIndicators);
  }

  getReport(filters: any = {}): Observable<TripReport> {
    // let params = new HttpParams();

    // // Adiciona filtros válidos como parâmetros de consulta
    // Object.entries(filters).forEach(([key, value]) => {
    //   if (value !== undefined && value !== null && value !== '') {
    //     params = params.set(key, value);
    //   }
    // });

    // // Faz a chamada GET para o endpoint do relatório de viagens
    // return this.http.get<TripReport>(`${API_URL}/trips/report`, {
    //   params,
    // });
    const MOCK_TRIP_REPORT: TripReport = {
      distributions: {
        byStatus: [
          { status: TripStatus.PLANEJADA, count: 3 },
          { status: TripStatus.EM_ANDAMENTO, count: 5 },
          { status: TripStatus.CONCLUIDA, count: 12 },
          { status: TripStatus.CANCELADA, count: 2 },
        ],
        byVehicle: [
          { vehiclePlate: 'ABC-1234', count: 8, totalCost: 3200 },
          { vehiclePlate: 'DEF-5678', count: 6, totalCost: 2500 },
          { vehiclePlate: 'DEF-5678', count: 6, totalCost: 2500 },
          { vehiclePlate: 'DEF-5678', count: 6, totalCost: 2500 },
          { vehiclePlate: 'DEF-5678', count: 6, totalCost: 2500 },
          { vehiclePlate: 'DEF-5678', count: 6, totalCost: 2500 },
          { vehiclePlate: 'DEF-5678', count: 6, totalCost: 2500 },
          { vehiclePlate: 'DEF-5678', count: 6, totalCost: 2500 },
          { vehiclePlate: 'DEF-5678', count: 6, totalCost: 2500 },
          { vehiclePlate: 'GHI-9012', count: 3, totalCost: 1000 },
        ],
        byDriver: [
          { driverName: 'João Silva', count: 7, totalCost: 2900 },
          { driverName: 'Maria Souza', count: 6, totalCost: 2100 },
          { driverName: 'Maria Souza', count: 6, totalCost: 2100 },
          { driverName: 'Maria Souza', count: 6, totalCost: 2100 },
          { driverName: 'Maria Souza', count: 6, totalCost: 2100 },
          { driverName: 'Maria Souza', count: 6, totalCost: 2100 },
          { driverName: 'Maria Souza', count: 6, totalCost: 2100 },
          { driverName: 'Carlos Lima', count: 4, totalCost: 1700 },
        ],
        byDestination: [
          { destination: 'São Paulo', totalTrips: 5, totalCost: 2000 },
          { destination: 'Campinas', totalTrips: 4, totalCost: 1600 },
          { destination: 'Campinas', totalTrips: 4, totalCost: 1600 },
          { destination: 'Campinas', totalTrips: 4, totalCost: 1600 },
          { destination: 'Campinas', totalTrips: 4, totalCost: 1600 },
          { destination: 'Campinas', totalTrips: 4, totalCost: 1600 },
          { destination: 'Campinas', totalTrips: 4, totalCost: 1600 },
          { destination: 'Santos', totalTrips: 3, totalCost: 900 },
        ],
      },
    };
    console.log('🧪 Retornando dados mockados de relatório de viagens...');
    return of(MOCK_TRIP_REPORT);
  }
}
