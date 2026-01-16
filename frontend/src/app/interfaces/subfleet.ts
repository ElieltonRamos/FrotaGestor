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


export interface SubfleetReportResponse {
  period: Period;
  summary: Summary;
  subfleets: SubfleetMetric[];
}

export interface Period {
  startDate: string;  // "2026-01-01"
  endDate: string;    // "2026-01-31"
}

export interface Summary {
  totalSubfleets: number;
  totalVehicles: number;
  overallEfficiency: number;  // R$/km médio geral (ex: 0.45)
}

export interface SubfleetMetric {
  // Identificação
  subfleetId: number;
  name: string;
  color: string;              // Hex: "#10B981"
  managerName: string;
  
  // Frota
  totalVehicles: number;
  activeVehicles: number;
  maintenanceVehicles: number;
  
  // Operacional
  totalTrips: number;
  totalDistanceKm: number;
  totalExpenses: number;
  
  // Métricas Derivadas
  costPerKm: number;           // totalExpenses / totalDistanceKm
  tripsPerVehicle: number;     // totalTrips / totalVehicles
  kmPerTrip: number;           // totalDistanceKm / totalTrips
  vehiclesActiveRate: number;  // (activeVehicles / totalVehicles) * 100
  
  // Detalhes Ricos
  topExpenseType: string;      // "Combustível", "Manutenção"
  avgVehicleAge: number;       // Anos
  
  vehiclesByType: Record<string, number>;  // { "Caminhão": 4, "Carro": 3 }
}