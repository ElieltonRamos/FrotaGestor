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
