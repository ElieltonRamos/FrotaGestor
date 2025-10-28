export interface GpsDevice {
  id?: number;                     // ID do dispositivo
  vehicleId?: number | null;       // Veículo vinculado (NULLABLE - permite dispositivos sem veículos)
  imei: string;                    // Identificador do GPS (obrigatório)
  latitude?: number;               // Última latitude (opcional para dispositivos novos)
  longitude?: number;              // Última longitude (opcional para dispositivos novos)
  dateTime?: string | null;        // Momento da leitura ISO string (opcional para dispositivos novos)
  speed?: number;                  // Velocidade (opcional)
  heading?: number;                // Direção (opcional)
  iconMapUrl?: string | null;      // Ícone para o mapa
  title?: string | null;           // Modelo + placa
  ignition?: boolean;              // Ignição ligada/desligada
}