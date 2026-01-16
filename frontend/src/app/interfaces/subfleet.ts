export interface Subfleet {
  id?: number;
  name: string;
  description?: string | null;
  vehicleCount?: number;
  activeVehicleCount?: number;
}

export interface PartialSubfleet {
  name?: string;
  description?: string | null;
}

export interface SubfleetReport {
  subfleetId: number;
  totalVehicles: number;
  activeVehicles: number;
  maintenanceVehicles: number;
  totalTrips: number;
  totalDistanceKm: number;
  totalExpenses: number;
}

export interface SubfleetIndicators {
  totalActive: number;
  totalVehicles: number;
  lastSubfleet?: Subfleet;
}
