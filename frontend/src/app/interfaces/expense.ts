export interface Expense {
  date: string;
  type: string;
  amount: number;
  description: string;
  id?: number;
  vehicleId?: number;
  driverId?: number;
  tripId?: number;
  liters?: number;
  pricePerLiter?: number;
  odometer?: number;
}
