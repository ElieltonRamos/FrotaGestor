export interface Expense {
  date: string;
  type: ExpenseType;
  amount: number;
  description: string;
  id?: number;
  vehicleId?: number | null;
  driverId?: number | null;
  vehiclePlate?: string | null;
  driverName?: string | null;
  tripId?: number | null;
  liters?: number;
  pricePerLiter?: number;
  odometer?: number;
}

export enum ExpenseType {
  COMBUSTIVEL = 'Combustivel',
  MANUTENCAO = 'Manutencao',
  ALIMENTACAO = 'Alimentacao',
  HOSPEDAGEM = 'Hospedagem',
  MULTAS = 'Multas',
  IMPOSTOS = 'Impostos',
  OUTROS = 'Outros',
}

export interface RefuelingIndicators {
  totalAmount: number;
  totalLiters: number;
  avgPricePerLiter: number;
  topDriver?: {
    name: string;
    count: number;
  };
  topVehicleByAmount?: {
    plate: string;
    amount: number;
  };
  topVehicleByLiters?: {
    plate: string;
    liters: number;
  };
  lastRefueling?: {
    date: string;
    plate: string;
  };
}

export interface MaintenanceIndicators {
  totalAmount: number;
  totalCount: number;
  mostCommonType: string;
  topVehicleByAmount: { plate: string; amount: number };
  lastMaintenance: { date: string; plate: string };
}

export interface ExpenseIndicators {
  totalAmount: number;
  totalCount: number;
  mostCommonType?: string;
  lastExpense?: {
    date: string;
    type: string;
    description: string;
  };
}
