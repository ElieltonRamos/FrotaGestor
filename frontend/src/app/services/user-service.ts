import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { API_URL } from './api-url';
import { Token } from '../interfaces/user';

@Injectable({ providedIn: 'root' })
export class UserService {
  private tokenKey = 'auth_token';
  private apiUrl = API_URL;


  constructor(private client: HttpClient) {}

  login(username: string, password: string) {
    return this.client.post<Token>(`${this.apiUrl}/users/login`, {
      username,
      password,
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
}
