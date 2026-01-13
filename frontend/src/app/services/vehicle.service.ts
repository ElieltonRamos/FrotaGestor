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
import { Driver } from '../interfaces/driver';
import { Trip } from '../interfaces/trip';
import { Expense } from '../interfaces/expense';

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

  delete(id: number | string): Observable<Message> {
    return this.http.delete<Message>(`${API_URL}/vehicles/${id}`);
  }

  getIndicators(filters: Record<string, any> = {}): Observable<VehicleIndicators> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });
    return this.http.get<VehicleIndicators>(`${API_URL}/vehicles/indicators`, { params });
  }

  getReport(filters: Record<string, any>): Observable<VehicleReport> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });
    return this.http.get<VehicleReport>(`${API_URL}/reports/vehicles`, { params });
  }

  /** 
   * 🔄 ATUALIZADO: Agora aceita startDate e endDate
   */
  getTripsByVehicle(
    vehicleId: number,
    page: number = 1,
    limit: number = 10,
    sortKey: string = 'id',
    sortAsc: boolean = true,
    startDate?: string,  // ✨ NOVO: formato ISO 8601 (YYYY-MM-DD)
    endDate?: string     // ✨ NOVO: formato ISO 8601 (YYYY-MM-DD)
  ): Observable<PaginatedResponse<Trip>> {
    let params = new HttpParams()
      .set('page', page)
      .set('limit', limit)
      .set('sortBy', sortKey)
      .set('order', sortAsc ? 'asc' : 'desc');

    // ✨ Adicionar filtros de data se fornecidos
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }

    return this.http.get<PaginatedResponse<Trip>>(
      `${API_URL}/vehicles/${vehicleId}/trips`,
      { params }
    );
  }

  /** 
   * 🔄 ATUALIZADO: Agora aceita startDate e endDate
   */
  getExpensesByVehicle(
    vehicleId: number,
    page: number = 1,
    limit: number = 10,
    sortKey: string = 'id',
    sortAsc: boolean = true,
    startDate?: string,  // ✨ NOVO: formato ISO 8601 (YYYY-MM-DD)
    endDate?: string     // ✨ NOVO: formato ISO 8601 (YYYY-MM-DD)
  ): Observable<PaginatedResponse<Expense>> {
    let params = new HttpParams()
      .set('page', page)
      .set('limit', limit)
      .set('sortBy', sortKey)
      .set('order', sortAsc ? 'asc' : 'desc');

    // ✨ Adicionar filtros de data se fornecidos
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }

    return this.http.get<PaginatedResponse<Expense>>(
      `${API_URL}/vehicles/${vehicleId}/expenses`,
      { params }
    );
  }

  /** 
   * 🔄 ATUALIZADO: Agora aceita startDate e endDate
   */
  getTopDriverByVehicle(
    vehicleId: number,
    startDate?: string,  // ✨ NOVO: formato ISO 8601 (YYYY-MM-DD)
    endDate?: string     // ✨ NOVO: formato ISO 8601 (YYYY-MM-DD)
  ): Observable<Driver> {
    let params = new HttpParams();

    // ✨ Adicionar filtros de data se fornecidos
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }

    return this.http.get<Driver>(
      `${API_URL}/vehicles/${vehicleId}/top-driver`,
      { params }
    );
  }

  /** 
   * 🆕 Novo método para buscar veículos por subfrota
   */
  getBySubfleet(
    subfleetId: number,
    page: number = 1,
    limit: number = 10
  ): Observable<PaginatedResponse<Vehicle>> {
    const params = new HttpParams()
      .set('page', page)
      .set('limit', limit)
      .set('subfleetIdFilter', subfleetId);

    return this.http.get<PaginatedResponse<Vehicle>>(`${API_URL}/vehicles`, {
      params,
    });
  }

  /** 
   * 🆕 Atribuir motorista padrão ao veículo
   */
  assignDefaultDriver(vehicleId: number, driverId: number): Observable<Message> {
    return this.http.patch<Message>(`${API_URL}/vehicles/${vehicleId}`, {
      defaultDriverId: driverId
    });
  }

  /** 
   * 🆕 Remover motorista padrão do veículo
   */
  removeDefaultDriver(vehicleId: number): Observable<Message> {
    return this.http.patch<Message>(`${API_URL}/vehicles/${vehicleId}`, {
      defaultDriverId: null
    });
  }

  /** 
   * 🆕 Atribuir veículo a uma subfrota
   */
  assignToSubfleet(vehicleId: number, subfleetId: number): Observable<Message> {
    return this.http.patch<Message>(`${API_URL}/vehicles/${vehicleId}`, {
      subfleetId: subfleetId
    });
  }

  /** 
   * 🆕 Remover veículo de uma subfrota
   */
  removeFromSubfleet(vehicleId: number): Observable<Message> {
    return this.http.patch<Message>(`${API_URL}/vehicles/${vehicleId}`, {
      subfleetId: null
    });
  }
}
