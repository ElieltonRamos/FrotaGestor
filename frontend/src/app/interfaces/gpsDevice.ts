export interface GpsDevice {
  id?: number; // ID do dispositivo
  vehicleId?: number | null; // Veículo vinculado (NULLABLE - permite dispositivos sem veículos)
  imei: string; // Identificador do GPS (obrigatório)
  latitude?: number; // Última latitude (opcional para dispositivos novos)
  longitude?: number; // Última longitude (opcional para dispositivos novos)
  dateTime?: string | null; // Momento da leitura ISO string (opcional para dispositivos novos)
  speed?: number; // Velocidade (opcional)
  heading?: number; // Direção (opcional)
  iconMapUrl?: string | null; // Ícone para o mapa
  title?: string | null; // Modelo + placa
  ignition?: boolean; // Ignição ligada/desligada
}

export interface GpsHistory {
  id: number;
  gpsDeviceId: number;
  vehicleId?: number | null;
  dateTime: string; // ISO 8601 string
  latitude: number;
  longitude: number;
  rawLog: string;
}

export interface ParsedGpsEvent {
  id: number;
  type: string;
  description: string;
  header: string;
  dateTime: string;
  latitude?: number;
  longitude?: number;
  speed: number;
  heading: number;
  battery?: number;
  ignition?: boolean;
}

export interface CommandRequest {
  commandType: string;
  deviceId: string;
  parameters?: Record<string, string>;
}

export interface CommandResponse {
  success: boolean;
  message: string;
  command?: string;
}
