// src/app/services/gps-device.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.url';
import { GpsDevice } from '../interfaces/gpsDevice';
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
        const filterKey = key === 'vehicleId' ? 'vehicleId' : key === 'imei' ? 'imei' : key;
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
}