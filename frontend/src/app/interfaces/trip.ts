export interface Trip {
  id?: number;
  vehicleId: number;
  driverId: number;
  startLocation?: string | null;
  endLocation?: string | null;
  startTime: string;
  endTime?: string | null;
  distanceKm?: number | null;
  status: TripStatus;
}

export enum TripStatus {
  PLANEJADA = 'PLANEJADA',
  EM_ANDAMENTO = 'EM_ANDAMENTO',
  CONCLUIDA = 'CONCLUIDA',
  CANCELADA = 'CANCELADA',
}

