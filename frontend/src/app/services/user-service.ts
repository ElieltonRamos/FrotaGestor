import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { API_URL } from './api-url';
import { Message, Token, TokenPayload } from '../interfaces/user';
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

  changePassword(newPassword: string) {
    const token = this.getToken() || '';

    const decodedToken = jwt.jwtDecode<TokenPayload>(token);
    const userId = decodedToken.id;

    return this.client.patch<Message>(`${this.apiUrl}/users/${userId}`, {
      newPassword,
    })
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
}
