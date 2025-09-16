import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Driver } from '../interfaces/driver';
import { API_URL } from './api-url';
import { PaginatedResponse } from '../interfaces/paginator';

@Injectable({
  providedIn: 'root',
})
export class DriverService {
  constructor(private http: HttpClient) {}

  // Criar motorista
  create(driver: Omit<Driver, 'id'>): Observable<Driver> {
    return this.http.post<Driver>(API_URL, driver);
  }

  // Listar motoristas com paginação
  getAll(page: number = 1, limit: number = 10): Observable<PaginatedResponse<Driver>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString());

    return this.http.get<PaginatedResponse<Driver>>(API_URL, { params });
  }

  // Buscar motorista por ID
  getById(id: number | string): Observable<Driver> {
    return this.http.get<Driver>(`${API_URL}/${id}`);
  }

  // Atualizar motorista
  update(id: number | string, driver: Partial<Driver>): Observable<Driver> {
    return this.http.put<Driver>(`${API_URL}/${id}`, driver);
  }

  // Deletar motorista
  delete(id: number | string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/${id}`);
  }
}
