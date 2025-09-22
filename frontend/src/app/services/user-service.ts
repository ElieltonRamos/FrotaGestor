import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { API_URL } from './api-url';
import { Token, TokenPayload } from '../interfaces/user';
import * as jwt from 'jwt-decode';

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

  /** 🔑 Retorna os dados do usuário contidos no token */
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
}
