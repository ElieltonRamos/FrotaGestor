// src/app/services/gps-device.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.url';
import { CommandRequest, CommandResponse, GpsDevice, GpsHistory, ParsedGpsEvent } from '../interfaces/gpsDevice';
import { Message } from '../interfaces/user';
import { PaginatedResponse } from '../interfaces/paginator';

@Injectable({
  providedIn: 'root',
})
export class GpsDeviceService {
  constructor(private http: HttpClient) {}

  /**
   * Cria um novo dispositivo GPS
   * @param device - Dados do dispositivo (imei e iconMapUrl são obrigatórios)
   */
  create(device: Partial<GpsDevice>): Observable<Message> {
    return this.http.post<Message>(`${API_URL}/gps-devices`, device);
  }

  /**
   * Atualiza um dispositivo GPS existente
   * @param id - ID do dispositivo
   * @param device - Campos a serem atualizados (pode incluir vehicleId: null para desvincular)
   */
  update(id: number, device: Partial<GpsDevice>): Observable<Message> {
    return this.http.patch<Message>(`${API_URL}/gps-devices/${id}`, device);
  }

  /**
   * Deleta um dispositivo GPS
   * IMPORTANTE: Só pode deletar dispositivos SEM veículo vinculado
   * @param id - ID do dispositivo
   */
  delete(id: number): Observable<Message> {
    return this.http.delete<Message>(`${API_URL}/gps-devices/${id}`);
  }

  /**
   * Busca todos os dispositivos GPS com paginação e filtros
   * @param page - Número da página
   * @param limit - Itens por página
   * @param filters - Filtros opcionais (imei, vehicleId, dateTime)
   * @param sortKey - Campo para ordenação (não implementado no backend ainda)
   * @param sortAsc - Ordem ascendente (não implementado no backend ainda)
   */
  getAll(
    page: number = 1,
    limit: number = 10,
    filters: Record<string, any> = {},
    sortKey: string = 'id',
    sortAsc: boolean = true
  ): Observable<PaginatedResponse<GpsDevice>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString());

    // Adiciona filtros suportados pelo backend
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        // Mapeia os nomes dos filtros para os esperados pelo backend
        const filterKey =
          key === 'vehicleId' ? 'vehicleId' : key === 'imei' ? 'imei' : key;
        params = params.set(filterKey, value.toString());
      }
    });

    // Nota: sortBy e order não estão implementados no backend ainda
    // O backend ordena por ID DESC por padrão

    return this.http.get<PaginatedResponse<GpsDevice>>(
      `${API_URL}/gps-devices`,
      { params }
    );
  }

  /**
   * Busca um dispositivo GPS por ID
   * @param id - ID do dispositivo
   */
  getById(id: number): Observable<GpsDevice> {
    return this.http.get<GpsDevice>(`${API_URL}/gps-devices/${id}`);
  }

  /**
   * Busca o dispositivo GPS vinculado a um veículo específico
   * @param vehicleId - ID do veículo
   */
  getGpsDeviceByVehicle(vehicleId: number): Observable<GpsDevice> {
    return this.http.get<GpsDevice>(
      `${API_URL}/gps-devices/vehicle/${vehicleId}`
    );
  }

  /**
   * Busca o histórico de posições de um dispositivo GPS com paginação e filtro por período
   * @param deviceId - ID do dispositivo GPS
   * @param page - Número da página (padrão: 1)
   * @param limit - Itens por página (padrão: 10)
   * @param startDate - Data/hora inicial (ISO string, opcional)
   * @param endDate - Data/hora final (ISO string, opcional)
   * @param sortAsc - Ordem ascendente por timestamp (padrão: false = DESC)
   */
  getHistoryDevice(
    deviceId: number,
    page: number = 1,
    limit: number = 10,
    startDate?: string,
    endDate?: string,
    sortAsc: boolean = false
  ): Observable<PaginatedResponse<GpsHistory>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('sortBy', 'timestamp')
      .set('order', sortAsc ? 'asc' : 'desc');

    // Adiciona apenas os filtros de data se fornecidos
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }

    return this.http.get<PaginatedResponse<GpsHistory>>(
      `${API_URL}/gps-devices/vehicle/${deviceId}/history`,
      { params }
    );
  }

  sendCommandDevice(request: CommandRequest): Observable<CommandResponse> {
    const url = `${API_URL}/gps-devices/commands`;
    return this.http.post<CommandResponse>(url, request)
  }

  parse(rawLog: string, id: number): ParsedGpsEvent | null {
    const fields = rawLog.split(';');
    if (fields.length < 6) return null; // ignora logs inválidos

    const header = fields[0];

    const eventMap: Record<string, { type: string; description: string }> = {
      ST300ALV: { type: 'Alive', description: 'Sinal de vida do dispositivo' },
      ST300GPS: { type: 'Posição', description: 'Relatório de posição GPS' },
      ST300STT: {
        type: 'Status',
        description: 'Relatório de status do veículo',
      },
      ST300ALT: { type: 'Alerta', description: 'Evento de alarme ou sensor' },
      ST300EMG: {
        type: 'Emergência',
        description: 'Emergência ou bateria desconectada',
      },
      ST300EVT: { type: 'Evento', description: 'Evento configurável' },
      ST300CMD: { type: 'Comando', description: 'Resposta a comando enviado' },
      ST300HB: { type: 'Heartbeat', description: 'Relatório periódico' },
      ST300IGN: { type: 'Ignição', description: 'Motor ligado/desligado' },
      ST300GP: { type: 'Posição', description: 'Atualização GPS' },
    };

    const info = eventMap[header];
    if (!info) return null; // <— ignora logs não mapeados

    const dateYMD = fields[4] || '';
    const timeHMS = fields[5] || '';
    const isoDate =
      dateYMD.length >= 8
        ? `${dateYMD.substring(0, 4)}-${dateYMD.substring(
            4,
            6
          )}-${dateYMD.substring(6, 8)}T${timeHMS}`
        : '';

    const latitude = parseFloat(fields[7]) || 0;
    const longitude = parseFloat(fields[8]) || 0;
    const speed = parseFloat(fields[9]) || 0;
    const heading = parseFloat(fields[10]) || 0;
    const ignition = fields[19] === '1';

    return {
      id,
      type: info.type,
      description: info.description,
      header,
      dateTime: isoDate ? new Date(isoDate).toLocaleString('pt-BR') : '-',
      latitude,
      longitude,
      speed,
      heading,
      ignition,
    };
  }
}
