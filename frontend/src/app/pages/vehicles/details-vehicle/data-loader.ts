import { ChangeDetectorRef } from '@angular/core';
import { VehicleService } from '../../../services/vehicle.service';
import { GpsDeviceService } from '../../../services/gps-device.service';

type DataSetKey = 'gpsEvents' | 'trips' | 'expenses';

export interface DataSet<T> {
  items: T[];
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}

export function createDataLoader(
  vehicleService: VehicleService,
  gpsService: GpsDeviceService,
  cdr: ChangeDetectorRef,
  dataSets: Record<DataSetKey, DataSet<any>>
) {
  return function loadData(type: DataSetKey, vehicleId: number, page?: number) {
    const currentPage = page ?? dataSets[type].page;
    const limit = dataSets[type].limit;

    const setData = (res: any, transform?: (d: any) => any) => {
      dataSets[type] = {
        items: transform ? res.data.map(transform) : res.data,
        page: res.page, // atualiza para refletir a página retornada pelo backend
        limit: res.limit,
        total: res.total,
        totalPages: res.totalPages,
      };
      cdr.detectChanges();
    };

    const handleError = () => (dataSets[type].items = []);

    const serviceMap = {
      gpsEvents: () =>
        gpsService
          .getHistoryDevice(vehicleId, currentPage, limit)
          .subscribe({
            next: (res) => setData(res, (e) => gpsService.parse(e.rawLog, e.id)),
            error: handleError,
          }),
      trips: () =>
        vehicleService
          .getTripsByVehicle(vehicleId, currentPage, limit)
          .subscribe({ next: setData, error: handleError }),
      expenses: () =>
        vehicleService
          .getExpensesByVehicle(vehicleId, currentPage, limit)
          .subscribe({ next: setData, error: handleError }),
    };
    serviceMap[type]();
  };
}

