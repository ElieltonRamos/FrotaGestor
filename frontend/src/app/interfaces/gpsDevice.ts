export interface GpsDevice {
  id?: number;                           // Int - opcional (vem do servidor)
  vehicleId?: number | null;             // Int? - nullable OK
  imei: string;                          // String - OBRIGATÓRIO
  latitude?: number;                     // Double = 0.0 - opcional
  longitude?: number;                    // Double = 0.0 - opcional
  dateTime?: string | null;              // LocalDateTime? - ISO string nullable
  speed?: number;                        // Double = 0.0 - opcional
  heading?: number;                      // Double = 0.0 - opcional
  iconMapUrl?: string | null;            // String? - nullable
  title?: string | null;                 // String? - nullable
  ignition?: boolean;                    // Boolean = false - opcional
  lastCommunication?: string | null;     // LocalDateTime? - ISO string nullable (NOVO)
  batteryVoltage?: number | null;
}

export interface GpsHistory {
  id: number;                            // Long - OBRIGATÓRIO
  gpsDeviceId: number;                   // Int - OBRIGATÓRIO (NOVO)
  vehicleId?: number | null;             // Int? - nullable
  dateTime: string;                      // LocalDateTime - ISO string OBRIGATÓRIO
  latitude: number;                      // Double - OBRIGATÓRIO
  longitude: number;                     // Double - OBRIGATÓRIO
  speed: number;                         // Double - OBRIGATÓRIO (NOVO)
  heading: number;                       // Double - OBRIGATÓRIO (NOVO)
  ignition: boolean;                     // Boolean - OBRIGATÓRIO (NOVO)
  satellites?: number | null;            // Int? - nullable (NOVO)
  gpsFixed: boolean;                     // Boolean - OBRIGATÓRIO (NOVO)
  gpsQuality: string;                    // String - OBRIGATÓRIO (NOVO)
  odometer?: number | null;              // Long? - nullable (NOVO)
  batteryVoltage?: number | null;        // Double? - nullable (NOVO)
  messageType: string;                   // String - OBRIGATÓRIO (NOVO)
  eventCode?: number | null;             // Int? - nullable (NOVO)
  rawLog: string;                        // String - OBRIGATÓRIO
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
