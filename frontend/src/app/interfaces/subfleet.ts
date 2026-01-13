export enum SubfleetStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE'
}

export interface Subfleet {
  id?: number;
  name: string;
  description?: string | null;
  parentId?: number | null;
  color: string;
  icon: string;
  managerUserId?: number | null;
  status: SubfleetStatus;

  // Campos de JOIN (não persistidos)
  parentName?: string | null;      // Nome da subfrota pai
  managerName?: string | null;     // Nome do gerente
  vehicleCount?: number;            // Contagem de veículos
  activeVehicleCount?: number;      // Veículos ativos

  // Metadata
  createdAt?: Date | string | null;
}

export interface PartialSubfleet {
  name?: string;
  description?: string | null;
  parentId?: number | null;
  color?: string;
  icon?: string;
  managerUserId?: number | null;
  status?: SubfleetStatus;
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

export interface CreateSubfleetRequest {
  name: string;
  description?: string;
  parentId?: number;
  color?: string;
  icon?: string;
  managerUserId?: number;
  status?: SubfleetStatus;
}

export interface SubfleetIndicators {
  totalActive: number;
  totalVehicles: number;
  lastSubfleet?: Subfleet;
}

// Valores padrão para criação (opcional)
export const DEFAULT_SUBFLEET_VALUES = {
  color: '#3B82F6',
  icon: 'truck',
  status: SubfleetStatus.ACTIVE,
  vehicleCount: 0,
  activeVehicleCount: 0
};
