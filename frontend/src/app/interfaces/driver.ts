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
  lastDriver: {
    name: string;
    cpf: string;
    date: string;
  };
}

export interface DriverReport {
  distributions: {
    totalDrivers: number; // total de motoristas
    cnhExpiringSoon: number; // CNHs vencendo nos próximos 30 dias
    cnhExpired: number; // CNHs vencidas
    byCategory: {
      category: string; // B, C, D, E
      count: number; // quantidade de motoristas
    }[];
  };
  driversStats: {
    driverName: string;
    driverId: number;
    totalTrips: number;
    totalDistance: number; // km
    totalCost: number; // R$
    averageFuelConsumption?: number; // litros/km
    lastTripDate?: string; // ISO string
  }[];
}
