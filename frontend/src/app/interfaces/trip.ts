export interface Trip {
  id?: number;
  vehicleId: number;
  driverId: number;
  status: TripStatus;
  startLocation?: string | null;
  endLocation?: string | null;
  startTime: string;
  endTime?: string | null;
  distanceKm?: number | null;
  vehiclePlate?: string | null;
  driverName?: string | null;
}

export enum TripStatus {
  PLANEJADA = 'PLANEJADA',
  EM_ANDAMENTO = 'EM_ANDAMENTO',
  CONCLUIDA = 'CONCLUIDA',
  CANCELADA = 'CANCELADA',
}

export interface TripIndicators {
  totalTrips: number;
  inProgress: number;
  completed: number;
  canceled: number;
  totalDistance: number;
  avgDistance: number;
  lastTrip: {
    date: string;
    driverName: string;
    vehiclePlate: string;
  };
}

export interface TripReport {
  distributions: {
    byStatus: { status: TripStatus; count: number }[];
    byVehicle: { vehiclePlate: string; count: number; totalCost: number }[];
    byDriver: { driverName: string; count: number; totalCost: number }[];
    byDestination: {
      destination: string;
      totalTrips: number;
      totalCost: number;
    }[];
  };
}
