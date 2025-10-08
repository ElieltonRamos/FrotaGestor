// vehicle.ts
export interface Vehicle {
  id?: number;
  plate: string;
  model: string;
  brand?: string | null;
  year?: number | null;
  status: VehicleStatus;
  iconMapUrl: string;
}

export enum VehicleStatus {
  ATIVO = 'ATIVO',
  INATIVO = 'INATIVO',
  MANUTENCAO = 'MANUTENCAO',
}

export interface VehicleIndicators {
  active: number;
  maintenance: number;
  lastVehicle: {
    plate: string;
    date: string;
  };
}

export interface VehicleReport {
  distributions: {
    byBrand: {
      brand: string;
      count: number;
    }[];
    byYear: {
      year: number;
      count: number;
    }[];
    byStatus: {
      status: 'ATIVO' | 'MANUTENCAO' | 'INATIVO';
      count: number;
    }[];
  };

  usageStats: {
    totalDistanceByVehicle: {
      plate: string;
      totalKm: number;
      totalTrips: number; // total de viagens lançadas para esse veículo
      topDriver?: {
        name: string;
        trips: number;
      }; // motorista que mais usou o veículo
      fuelCost: number; // gasto total com combustível
      maintenanceCost: number; // gasto total com manutenção
      totalCost: number; // gasto total do veículo
      lastMaintenanceDate?: string; // data da manutenção mais recente
      isInUse?: boolean; // true se o veículo estiver em movimento no momento
    }[];
    fuelConsumptionByVehicle: {
      plate: string;
      litersPerKm: number;
    }[];
  };
}
