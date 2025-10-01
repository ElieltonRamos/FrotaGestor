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
