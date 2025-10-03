// vehicle.ts
export interface Vehicle {
  id?: number
  plate: string;
  model: string;    
  brand?: string | null;
  year?: number | null;
  status: VehicleStatus;
}

export enum VehicleStatus {
  ATIVO = 'ATIVO',
  INATIVO = 'INATIVO',
  MANUTENCAO = 'MANUTENCAO',
}

export interface VehicleIndicators {
  total: number;
  active: number;
  maintenance: number;
  lastVehicle?: {
    plate: string;
    date: string;
  };
}
