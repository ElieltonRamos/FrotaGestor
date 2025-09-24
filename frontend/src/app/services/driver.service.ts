import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Driver } from '../interfaces/driver';
import { API_URL } from './api.url';
import { PaginatedResponse } from '../interfaces/paginator';
import { Message } from '../interfaces/user';

@Injectable({
  providedIn: 'root',
})
export class DriverService {
  constructor(private http: HttpClient) {}

  create(driver: Omit<Driver, 'id'>): Observable<Message> {
    return this.http.post<Message>(`${API_URL}/drivers`, driver);
  }

  getAll(
    page: number = 1,
    limit: number = 10,
    filters: any = {},
    sortKey: string = 'nome',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<Driver>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('sort', sortKey)
      .set('order', sortAsc ? 'asc' : 'desc');

    if (filters.name) {
      params = params.set('name', filters.name);
    }
    if (filters.cpf) {
      params = params.set('cpf', filters.cpf);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    
    const url = `${API_URL}/drivers?${params.toString()}`;
    console.log(url)
    return this.http.get<PaginatedResponse<Driver>>(url);
  }

  // Buscar motorista por ID
  getById(id: number | string): Observable<Driver> {
    return this.http.get<Driver>(`${API_URL}/drivers/${id}`);
  }

  // Atualizar motorista
  update(id: number | string, driver: Partial<Driver>): Observable<any> {
    console.log(driver)
    return this.http.patch(`${API_URL}/drivers/${id}`, driver);
  }

  // Deletar motorista
  delete(id: number | string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/drivers/${id}`);
  }
}
