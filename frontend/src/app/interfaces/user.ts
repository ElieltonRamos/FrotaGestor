export default interface User {
  id?: number;
  username: string;
  password: string;
  role?: string;
}

export interface Token {
  token: string;
}

export interface TokenPayload {
  id: number;
  username: string;
  role: string;
}
