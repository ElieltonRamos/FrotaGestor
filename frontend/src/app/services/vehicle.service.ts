import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { delay, Observable, of } from 'rxjs';
import { API_URL } from './api.url';
import {
  Vehicle,
  VehicleIndicators,
  VehicleReport,
} from '../interfaces/vehicle';
import { PaginatedResponse } from '../interfaces/paginator';
import { Message } from '../interfaces/user';
import { CustomMarker } from '../components/map-component/map-component';
import { Driver } from '../interfaces/driver';
import { Trip } from '../interfaces/trip';
import { Expense } from '../interfaces/expense';

const MOCK_INDICATORS: VehicleIndicators = {
  active: 18,
  maintenance: 5,
  lastVehicle: { plate: 'XYZ-9999', date: '2025-10-01' },
};

const MOCK_REPORT: VehicleReport = {
  distributions: {
    byBrand: [
      { brand: 'Toyota', count: 6 },
      { brand: 'Ford', count: 5 },
      { brand: 'Honda', count: 4 },
      { brand: 'Chevrolet', count: 3 },
    ],
    byYear: [
      { year: 2023, count: 4 },
      { year: 2022, count: 5 },
      { year: 2021, count: 4 },
      { year: 2020, count: 5 },
    ],
    byStatus: [
      { status: 'ATIVO', count: 18 },
      { status: 'MANUTENCAO', count: 5 },
    ],
  },
  usageStats: {
    totalDistanceByVehicle: [
      {
        plate: 'ABC-1234',
        totalKm: 12000,
        totalTrips: 45,
        topDriver: { name: 'João Silva', trips: 20 },
        fuelCost: 4500,
        maintenanceCost: 1200,
        totalCost: 5700,
        lastMaintenanceDate: '2025-09-20',
        isInUse: true,
      },
      {
        plate: 'DEF-5678',
        totalKm: 8000,
        totalTrips: 30,
        topDriver: { name: 'Maria Souza', trips: 15 },
        fuelCost: 3000,
        maintenanceCost: 900,
        totalCost: 3900,
        lastMaintenanceDate: '2025-08-10',
        isInUse: false,
      },
      {
        plate: 'GHI-9012',
        totalKm: 15000,
        totalTrips: 50,
        topDriver: { name: 'Carlos Lima', trips: 25 },
        fuelCost: 6000,
        maintenanceCost: 1500,
        totalCost: 7500,
        lastMaintenanceDate: '2025-09-15',
        isInUse: true,
      },
      {
        plate: 'JKL-3456',
        totalKm: 5000,
        totalTrips: 20,
        topDriver: { name: 'Ana Paula', trips: 10 },
        fuelCost: 2000,
        maintenanceCost: 600,
        totalCost: 2600,
        lastMaintenanceDate: '2025-08-30',
        isInUse: false,
      },
    ],
    fuelConsumptionByVehicle: [
      { plate: 'ABC-1234', litersPerKm: 0.12 },
      { plate: 'DEF-5678', litersPerKm: 0.15 },
      { plate: 'GHI-9012', litersPerKm: 0.11 },
      { plate: 'JKL-3456', litersPerKm: 0.14 },
    ],
  },
};

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

  getIndicators(filters: any = {}): Observable<VehicleIndicators> {
    // return do backend
    // let params = new HttpParams();
    // Object.entries(filters).forEach(([key, value]) => {
    //   if (value !== undefined && value !== null && value !== '') {
    //     params = params.set(key, value);
    //   }
    // });
    // return this.http.get<VehicleIndicators>(`${API_URL}/vehicles/indicators`, { params });

    // Mock
    console.log('🧪 Retornando dados mockados de indicadores de veículos...');
    return of(MOCK_INDICATORS);
  }

  getReport(filters: any = {}): Observable<VehicleReport> {
    // return do backend
    // let params = new HttpParams();
    // Object.entries(filters).forEach(([key, value]) => {
    //   if (value !== undefined && value !== null && value !== '') {
    //     params = params.set(key, value);
    //   }
    // });
    // return this.http.get<VehicleReport>(`${API_URL}/vehicles/report`, { params });

    // Mock
    console.log('🧪 Retornando dados mockados de relatório de veículos...');
    return of(MOCK_REPORT);
  }

  getLocationsVehicles(): Observable<CustomMarker[]> {
    const MOCK_LOCATIONS: CustomMarker[] = [
      {
        lat: -14.9495,
        lng: -42.841,
        color: '#10B981',
        title: 'Veículo 1',
        description: 'Veículo ativo próximo ao fallback.',
        vehicleId: 1,
      },
      {
        lat: -14.9492,
        lng: -42.8395,
        iconUrl: 'assets/truck-icon.png',
        title: 'Caminhão A',
        description: 'Veículo em rota de entrega próxima.',
        vehicleId: 2,
      },
      {
        lat: -14.9485,
        lng: -42.8408,
        color: '#3B82F6',
        title: 'Veículo 2',
        description: 'Outro veículo próximo ao fallback.',
        vehicleId: 3,
      },
    ];

    // Caso queira consumir backend, descomente abaixo e ajuste endpoint:
    // return this.http.get<CustomMarker[]>(`${API_URL}/vehicles/locations`);

    console.log('🧪 Retornando dados mockados de localização de veículos...');
    return of(MOCK_LOCATIONS);
  }

  getTripsByVehicle(
    vehicleId: number,
    page: number = 1,
    limit: number = 10,
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<Trip>> {
    const params = new HttpParams()
      .set('page', page)
      .set('limit', limit)
      .set('sortBy', sortKey)
      .set('order', sortAsc ? 'asc' : 'desc');

    return this.http.get<PaginatedResponse<Trip>>(
      `${API_URL}/vehicles/${vehicleId}/trips`,
      { params }
    );
  }

  /** 🔹 Lista as despesas relacionadas a um veículo */
  getExpensesByVehicle(
    vehicleId: number,
    page: number = 1,
    limit: number = 10,
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<Expense>> {
    const params = new HttpParams()
      .set('page', page)
      .set('limit', limit)
      .set('sortBy', sortKey)
      .set('order', sortAsc ? 'asc' : 'desc');

    return this.http.get<PaginatedResponse<Expense>>(
      `${API_URL}/vehicles/${vehicleId}/expenses`,
      { params }
    );
  }

  getTopDriverByVehicle(vehicleId: number) {
    return this.http.get<Driver>(`${API_URL}/vehicles/${vehicleId}/top-driver`);
  }
}
