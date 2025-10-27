export interface GpsDevice {
  id?: number;                // ID do dispositivo
  vehicleId: number;         // Veículo vinculado
  imei: string;              // Identificador do GPS
  latitude: number;          // Última latitude
  longitude: number;         // Última longitude
  dateTime: string;          // Momento da leitura (ISO string)
  speed?: number;            // Velocidade (opcional)
  heading?: number;          // Direção (opcional)
  iconMapUrl?: string | null;// Ícone para o mapa
  title?: string | null;     // Modelo + placa
  ignition?: boolean;        // Ignição ligada/desligada
}

export interface GpsDeviceIndicators {
  active: number;         // Número de dispositivos ativos
  lastDevice?: {         // Último dispositivo cadastrado
    imei: string;        // IMEI do último dispositivo
    dateTime: string;    // Data e hora do último cadastro (ISO string)
  };
}