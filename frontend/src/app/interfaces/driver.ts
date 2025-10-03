export interface Driver {
  id?: number;
  name: string;
  cpf: string;
  cnh: string;
  cnhCategory?: string;
  cnhExpiration?: Date;
  phone?: string;
  email?: string;
  status: DriverStatus;
  deletedAt?: Date;
}

export type DriverStatus = 'ATIVO' | 'INATIVO';

export interface DriverIndicators {
  total: number;
  withExpiredLicense: number;
  withExpiringLicense: number;
  mostCommonCategory?: string;
  lastDriver?: {
    name: string;
    cpf: string;
    date: string;
  };
}
