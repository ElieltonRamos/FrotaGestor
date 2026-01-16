import { ChangeDetectorRef } from '@angular/core';
import { SubfleetService } from '../../../services/subfleet.service';
import { TripService } from '../../../services/trip.service';
import { ExpenseService } from '../../../services/expense.service';
import { GpsDeviceService } from '../../../services/gps-device.service';
import { GpsDevice, GpsHistory } from '../../../interfaces/gpsDevice';

type DataSetKey = 'gpsEvents' | 'trips' | 'expenses';

export interface DataSet<T> {
  items: T[];
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}

export interface DateFilters {
  startDate?: string;
  endDate?: string;
}

export interface DataLoader {
  loadData: (
    type: DataSetKey,
    subfleetId: number,
    page?: number,
    dateFilters?: DateFilters
  ) => void;
  loadMarkers: (subfleetId: number) => void;
  loadMapHistory: (
    subfleetId: number,
    startDateTime?: string,
    endDateTime?: string
  ) => void;
}

export function createDataLoader(
  subfleetService: SubfleetService,
  tripService: TripService,
  expenseService: ExpenseService,
  gpsService: GpsDeviceService,
  cdr: ChangeDetectorRef,
  dataSets: Record<DataSetKey, DataSet<any>>,
  markers: GpsDevice[],
  mapPoints: GpsHistory[]
): DataLoader {
  const setData = (dataset: DataSet<any>, res: any) => {
    dataset.items = res.data;
    dataset.page = res.page;
    dataset.limit = res.limit;
    dataset.total = res.total;
    dataset.totalPages = res.totalPages;
    cdr.detectChanges();
  };

  const handleError = (dataset: DataSet<any>) => {
    dataset.items = [];
  };

  const loadData = (
    type: DataSetKey,
    subfleetId: number,
    page?: number,
    dateFilters?: DateFilters
  ) => {
    const currentPage = page ?? dataSets[type].page;
    const limit = dataSets[type].limit;

    const serviceMap = {
      gpsEvents: () =>
        gpsService
          .getGpsHistoryBySubfleet(
            subfleetId,
            currentPage,
            limit,
            dateFilters?.startDate,
            dateFilters?.endDate
          )
          .subscribe({
            next: (res) => setData(dataSets.gpsEvents, res),
            error: () => handleError(dataSets.gpsEvents),
          }),
      trips: () =>
        tripService
          .getTripsBySubfleet(subfleetId, currentPage, limit)
          .subscribe({
            next: (res) => setData(dataSets.trips, res),
            error: () => handleError(dataSets.trips),
          }),
      expenses: () =>
        expenseService
          .getExpensesBySubfleet(subfleetId, currentPage, limit)
          .subscribe({
            next: (res) => setData(dataSets.expenses, res),
            error: () => handleError(dataSets.expenses),
          }),
    };
    serviceMap[type]();
  };

  const loadMarkers = (subfleetId: number) => {
    gpsService.getGpsDevicesBySubfleet(subfleetId).subscribe({
      next: (devices) => {
        markers.splice(0, markers.length, ...devices);
        cdr.detectChanges();
      },
      error: () => {
        markers.length = 0;
        cdr.detectChanges();
      },
    });
  };


  const loadMapHistory = (
    subfleetId: number,
    startDateTime?: string,
    endDateTime?: string
  ) => {
    gpsService
      .getGpsHistoryBySubfleet(subfleetId, 1, 200, startDateTime, endDateTime)
      .subscribe({
        next: (res) => {
          mapPoints.splice(
            0,
            mapPoints.length,
            ...res.data.filter(
              (p: GpsHistory) => p.latitude !== 0 && p.longitude !== 0
            )
          );
          cdr.detectChanges();
        },
      });
  };

  return { loadData, loadMarkers, loadMapHistory };
}
