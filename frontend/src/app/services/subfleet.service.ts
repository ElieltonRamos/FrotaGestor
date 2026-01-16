import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { delay, Observable, of } from 'rxjs';
import { API_URL } from './api.url';
import {
  Subfleet,
  PartialSubfleet,
  SubfleetReport,
  SubfleetIndicators,
  SubfleetReportResponse,
  Period,
} from '../interfaces/subfleet';
import { PaginatedResponse } from '../interfaces/paginator';
import { Message } from '../interfaces/user';
import { Vehicle } from '../interfaces/vehicle';

@Injectable({
  providedIn: 'root',
})
export class SubfleetService {
  constructor(private http: HttpClient) {}

  /**
   * Criar nova subfrota
   */
  create(subfleet: PartialSubfleet): Observable<Subfleet> {
    return this.http.post<Subfleet>(`${API_URL}/subfleets`, subfleet);
  }

  /**
   * Listar subfrotas com filtros e paginação
   */
  getAll(
    page: number = 1,
    limit: number = 10,
    filters: {
      status?: string;
      name?: string;
      parentId?: number;
      managerUserId?: number;
    } = {}
  ): Observable<PaginatedResponse<Subfleet>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString());

    // Adicionar filtros opcionais
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.parentId !== undefined) {
      params = params.set('parentId', filters.parentId.toString());
    }
    if (filters.managerUserId !== undefined) {
      params = params.set('managerUserId', filters.managerUserId.toString());
    }
    if (filters.name !== undefined) {
      params = params.set('name', filters.name.toString());
    }

    return this.http.get<PaginatedResponse<Subfleet>>(`${API_URL}/subfleets`, {
      params,
    });
  }

  /**
   * Buscar subfrota por ID
   */
  getById(id: number): Observable<Subfleet> {
    return this.http.get<Subfleet>(`${API_URL}/subfleets/${id}`);
  }

  /**
   * Atualizar subfrota
   */
  update(id: number, subfleet: PartialSubfleet): Observable<Message> {
    return this.http.patch<Message>(`${API_URL}/subfleets/${id}`, subfleet);
  }

  /**
   * Deletar subfrota
   */
  delete(id: number): Observable<Message> {
    return this.http.delete<Message>(`${API_URL}/subfleets/${id}`);
  }

  /**
   * Obter relatório da subfrota
   */
  getReport(subfleetId: number): Observable<SubfleetReport> {
    return this.http.get<SubfleetReport>(
      `${API_URL}/subfleets/${subfleetId}/report`
    );
  }

  /**
   * Listar veículos de uma subfrota específica
   */
  getVehiclesBySubfleet(
    subfleetId: number,
    page: number = 1,
    limit: number = 10
  ): Observable<PaginatedResponse<Vehicle>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString());

    return this.http.get<PaginatedResponse<Vehicle>>(
      `${API_URL}/subfleets/${subfleetId}/vehicles`,
      {
        params,
      }
    );
  }

  getIndicators(): Observable<SubfleetIndicators> {
    return this.http.get<SubfleetIndicators>(`${API_URL}/subfleets/indicators`);
  }

  // getSubfleetReport(
  //   period: Period,
  //   subfleetId?: number
  // ): Observable<SubfleetReportResponse> {
  //   let params = new HttpParams()
  //     .set('startDate', period.startDate)
  //     .set('endDate', period.endDate);

  //   if (subfleetId) {
  //     params = params.set('subfleetId', subfleetId.toString());
  //   }

  //   return this.http.get<SubfleetReportResponse>(API_URL, { params });
  // }

  /**
   * MOCK MODE: Retorna dados estáticos para teste
   * Comente esta linha para usar API real:
   * return this.http.get<SubfleetReportResponse>(this.API_URL, { params });
   */
  getSubfleetReport(period: any, subfleetId?: number): Observable<SubfleetReportResponse> {
    return of(this.getMockData()).pipe(delay(800)); // Simula loading 800ms
  }

  private getMockData(): SubfleetReportResponse {
    return {
      period: {
        startDate: "2026-01-01",
        endDate: "2026-01-31"
      },
      summary: {
        totalSubfleets: 5,
        totalVehicles: 28,
        overallEfficiency: 0.48
      },
      subfleets: [
        {
          subfleetId: 1,
          name: "Sul",
          color: "#10B981",
          managerName: "Carlos Silva",
          totalVehicles: 8,
          activeVehicles: 7,
          maintenanceVehicles: 1,
          totalTrips: 45,
          totalDistanceKm: 2500.5,
          totalExpenses: 1250.0,
          costPerKm: 0.50,
          tripsPerVehicle: 5.63,
          kmPerTrip: 55.57,
          vehiclesActiveRate: 87.5,
          topExpenseType: "Combustível",
          avgVehicleAge: 3.2,
          vehiclesByType: { "Caminhão": 4, "Carro": 3, "Moto": 1 }
        },
        {
          subfleetId: 2,
          name: "Nordeste",
          color: "#EF4444",
          managerName: "Maria Santos",
          totalVehicles: 6,
          activeVehicles: 5,
          maintenanceVehicles: 1,
          totalTrips: 32,
          totalDistanceKm: 1800.0,
          totalExpenses: 936.0,
          costPerKm: 0.52,
          tripsPerVehicle: 5.33,
          kmPerTrip: 56.25,
          vehiclesActiveRate: 83.3,
          topExpenseType: "Manutenção",
          avgVehicleAge: 2.8,
          vehiclesByType: { "Carro": 4, "Caminhão": 2 }
        },
        {
          subfleetId: 3,
          name: "Oeste",
          color: "#F59E0B",
          managerName: "João Pereira",
          totalVehicles: 7,
          activeVehicles: 4,
          maintenanceVehicles: 3,
          totalTrips: 28,
          totalDistanceKm: 1500.0,
          totalExpenses: 930.0,
          costPerKm: 0.62,
          tripsPerVehicle: 4.0,
          kmPerTrip: 53.57,
          vehiclesActiveRate: 57.1,
          topExpenseType: "Manutenção",
          avgVehicleAge: 4.1,
          vehiclesByType: { "Caminhão": 5, "Carro": 2 }
        },
        {
          subfleetId: 4,
          name: "Leste",
          color: "#8B5CF6",
          managerName: "Ana Costa",
          totalVehicles: 4,
          activeVehicles: 4,
          maintenanceVehicles: 0,
          totalTrips: 22,
          totalDistanceKm: 1200.0,
          totalExpenses: 420.0,
          costPerKm: 0.35,
          tripsPerVehicle: 5.5,
          kmPerTrip: 54.55,
          vehiclesActiveRate: 100.0,
          topExpenseType: "Combustível",
          avgVehicleAge: 2.5,
          vehiclesByType: { "Carro": 3, "Moto": 1 }
        },
        {
          subfleetId: 5,
          name: "Centro",
          color: "#06B6D4",
          managerName: "Pedro Lima",
          totalVehicles: 3,
          activeVehicles: 2,
          maintenanceVehicles: 1,
          totalTrips: 15,
          totalDistanceKm: 800.0,
          totalExpenses: 400.0,
          costPerKm: 0.50,
          tripsPerVehicle: 5.0,
          kmPerTrip: 53.33,
          vehiclesActiveRate: 66.7,
          topExpenseType: "Multas",
          avgVehicleAge: 3.5,
          vehiclesByType: { "Carro": 2, "Moto": 1 }
        }
      ]
    };
  }

  // Versão real (comente o mock acima para ativar)
  /*
  getSubfleetReport(period: any, subfleetId?: number): Observable<SubfleetReportResponse> {
    let params = new HttpParams()
      .set('startDate', period.startDate)
      .set('endDate', period.endDate);
    if (subfleetId) params = params.set('subfleetId', subfleetId.toString());
    return this.http.get<SubfleetReportResponse>(this.API_URL, { params });
  }
  */

  getDefaultPeriod() {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(endDate.getDate() - 30);
    return {
      startDate: startDate.toISOString().split('T')[0],
      endDate: endDate.toISOString().split('T')[0]
    };
  }

  getDefaultReport(subfleetId?: number): Observable<SubfleetReportResponse> {
    return this.getSubfleetReport(this.getDefaultPeriod(), subfleetId);
  }
}
