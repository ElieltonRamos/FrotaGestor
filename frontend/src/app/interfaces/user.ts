export default interface User {
  id?: number;
  username: string;
  password?: string;
  role?: string;
}

export interface UserIndicators {
  totalUsers: number;
  admins: number;
  regulars: number;
  lastUser?: { username: string; createdAt: string };
}

export enum UserRole {
  ADMIN = 'ADMIN',
  USER = 'USER',
}

export interface Token {
  token: string;
}

export interface TokenPayload {
  userId: number;
  username: string;
  role: string;
}

export interface Message {
  message: string;
}
