import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.url';
import {
  Subfleet,
  CreateSubfleetRequest,
  PartialSubfleet,
  SubfleetReport,
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
  create(subfleet: CreateSubfleetRequest): Observable<Subfleet> {
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
      params = params.set(
        'managerUserId',
        filters.managerUserId.toString()
      );
    }
    if (filters.name !== undefined) {
      params = params.set(
        'name',
        filters.name.toString()
      );
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
      .set('limit', limit.toString())
      .set('subfleetIdFilter', subfleetId.toString());

    return this.http.get<PaginatedResponse<Vehicle>>(`${API_URL}/vehicles`, {
      params,
    });
  }

  /**
   * Listar subfrotas raiz (sem pai)
   */
  getRootSubfleets(
    page: number = 1,
    limit: number = 10
  ): Observable<PaginatedResponse<Subfleet>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('parentIdFilter', 'null'); // Filtrar apenas subfrotas sem pai

    return this.http.get<PaginatedResponse<Subfleet>>(`${API_URL}/subfleets`, {
      params,
    });
  }

  /**
   * Listar subfrotas filhas de uma subfrota específica
   */
  getChildSubfleets(
    parentId: number,
    page: number = 1,
    limit: number = 10
  ): Observable<PaginatedResponse<Subfleet>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('parentIdFilter', parentId.toString());

    return this.http.get<PaginatedResponse<Subfleet>>(`${API_URL}/subfleets`, {
      params,
    });
  }

  /**
   * Listar subfrotas por gerente
   */
  getByManager(
    managerUserId: number,
    page: number = 1,
    limit: number = 10
  ): Observable<PaginatedResponse<Subfleet>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('managerUserIdFilter', managerUserId.toString());

    return this.http.get<PaginatedResponse<Subfleet>>(`${API_URL}/subfleets`, {
      params,
    });
  }

  /**
   * Atribuir gerente a uma subfrota
   */
  assignManager(
    subfleetId: number,
    managerUserId: number
  ): Observable<Message> {
    return this.http.patch<Message>(`${API_URL}/subfleets/${subfleetId}`, {
      managerUserId: managerUserId,
    });
  }

  /**
   * Remover gerente de uma subfrota
   */
  removeManager(subfleetId: number): Observable<Message> {
    return this.http.patch<Message>(`${API_URL}/subfleets/${subfleetId}`, {
      managerUserId: null,
    });
  }

  /**
   * Mudar status da subfrota
   */
  updateStatus(
    subfleetId: number,
    status: 'ACTIVE' | 'INACTIVE'
  ): Observable<Message> {
    return this.http.patch<Message>(`${API_URL}/subfleets/${subfleetId}`, {
      status: status,
    });
  }

  /**
   * Mover subfrota para outra subfrota pai
   */
  moveToParent(
    subfleetId: number,
    newParentId: number | null
  ): Observable<Message> {
    return this.http.patch<Message>(`${API_URL}/subfleets/${subfleetId}`, {
      parentId: newParentId,
    });
  }

  /**
   * Listar subfrotas ativas (helper method)
   */
  getActiveSubfleets(
    page: number = 1,
    limit: number = 100
  ): Observable<PaginatedResponse<Subfleet>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('statusFilter', 'ACTIVE');

    return this.http.get<PaginatedResponse<Subfleet>>(`${API_URL}/subfleets`, {
      params,
    });
  }
}
