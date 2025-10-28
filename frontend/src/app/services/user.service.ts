import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from './api.url';
import User, { Message, Token, TokenPayload } from '../interfaces/user';
import { PaginatedResponse } from '../interfaces/paginator';
import * as jwt from 'jwt-decode';

export interface UserIndicators {
  totalUsers: number;
  admins: number;
  regulars: number;
  lastUser?: { username: string; createdAt: string };
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private tokenKey = 'auth_token';
  private apiUrl = API_URL;

  constructor(private client: HttpClient) {}

  login(username: string, password: string): Observable<Token> {
    return this.client.post<Token>(`${this.apiUrl}/users/login`, {
      username,
      password,
    });
  }

  changePassword(newPassword: string): Observable<Message> {
    const token = this.getToken() || '';
    const decodedToken = jwt.jwtDecode<TokenPayload>(token);
    const userId = decodedToken.userId;

    return this.client.patch<Message>(`${this.apiUrl}/users/${userId}`, {
      password: newPassword,
    });
  }

  logout() {
    localStorage.removeItem(this.tokenKey);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getUserInfo(): TokenPayload | null {
    const token = this.getToken();
    if (!token) return null;

    try {
      return jwt.jwtDecode<TokenPayload>(token);
    } catch (e) {
      console.error('Erro ao decodificar token', e);
      return null;
    }
  }

  create(user: User): Observable<Message> {
    return this.client.post<Message>(`${this.apiUrl}/users`, user);
  }

  getAll(
    page: number = 1,
    limit: number = 10,
    filters: Record<string, any> = {},
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<User>> {
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

    const url = `${this.apiUrl}/users?${params.toString()}`;
    return this.client.get<PaginatedResponse<User>>(url);
  }

  getById(id: number | string): Observable<User> {
    return this.client.get<User>(`${this.apiUrl}/users/${id}`);
  }

  update(id: number | string, user: Partial<User>): Observable<Message> {
    return this.client.patch<Message>(`${this.apiUrl}/users/${id}`, user);
  }

  delete(id: number | string): Observable<void> {
    return this.client.delete<void>(`${this.apiUrl}/users/${id}`);
  }

  getIndicators(filters: Record<string, any> = {}): Observable<UserIndicators> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });

    return this.client.get<UserIndicators>(`${this.apiUrl}/users/indicators`, {
      params,
    });
  }
}
