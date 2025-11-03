import { ChangeDetectorRef, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { VehicleService } from '../../../services/vehicle.service';
import { Vehicle, VehicleStatus } from '../../../interfaces/vehicle';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { MapComponent } from '../../../components/map-component/map-component';
import {
  BaseListComponent,
  ColumnConfig,
} from '../../../components/base-list-component/base-list-component';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';
import { CommonModule } from '@angular/common';
import { Driver } from '../../../interfaces/driver';
import { Trip } from '../../../interfaces/trip';
import { Expense } from '../../../interfaces/expense';
import {
  GpsDevice,
  GpsHistory,
  ParsedGpsEvent,
} from '../../../interfaces/gpsDevice';
import { GpsDeviceService } from '../../../services/gps-device.service';

const mockHistory = [
  {
    "id": 1,
    "gpsDeviceId": 1,
    "vehicleId": 4,
    "dateTime": "2025-11-03T13:20:00",
    "latitude": -14.850509,
    "longitude": -42.868326,
    "rawLog": "ST300EMG;807452267;145;407;20251103;13:20:00;8f8218;-14.850509;-042.868326;000.000;325.25;9;1;46886790;12.00;000000;7;080059;4.1;1"
  },
  {
    "id": 2,
    "gpsDeviceId": 1,
    "vehicleId": 4,
    "dateTime": "2025-11-03T13:28:00",
    "latitude": -14.909814,
    "longitude": -42.833833,
    "rawLog": "ST300EMG;807452267;145;407;20251103;13:28:00;8f8218;-14.909814;-042.833833;000.000;325.25;9;1;46886790;12.00;000000;7;080059;4.1;1"
  },
  {
    "id": 3,
    "gpsDeviceId": 1,
    "vehicleId": 4,
    "dateTime": "2025-11-03T13:36:00",
    "latitude": -14.942736,
    "longitude": -42.835507,
    "rawLog": "ST300EMG;807452267;145;407;20251103;13:36:00;8f8218;-14.942736;-042.835507;000.000;325.25;9;1;46886790;12.00;000000;7;080059;4.1;1"
  },
  {
    "id": 4,
    "gpsDeviceId": 1,
    "vehicleId": 4,
    "dateTime": "2025-11-03T13:44:00",
    "latitude": -14.991400,
    "longitude": -42.849798,
    "rawLog": "ST300EMG;807452267;145;407;20251103;13:44:00;8f8218;-14.991400;-042.849798;000.000;325.25;9;1;46886790;12.00;000000;7;080059;4.1;1"
  },
  {
    "id": 5,
    "gpsDeviceId": 1,
    "vehicleId": 4,
    "dateTime": "2025-11-03T13:52:00",
    "latitude": -15.050526,
    "longitude": -42.839543,
    "rawLog": "ST300EMG;807452267;145;407;20251103;13:52:00;8f8218;-15.050526;-042.839543;000.000;325.25;9;1;46886790;12.00;000000;7;080059;4.1;1"
  }
]

@Component({
  selector: 'app-details-vehicle',
  standalone: true,
  imports: [CommonModule, MapComponent, BaseListComponent, PaginatorComponent],
  templateUrl: './details-vehicle.html',
})
export class DetailsVehicle {
  private route = inject(ActivatedRoute);
  private serviceVehicle = inject(VehicleService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private serviceGpsDevice = inject(GpsDeviceService);

  vehicle?: Vehicle;
  loading = false;
  topDriver?: Driver;
  markers: GpsDevice[] = [];
  gpsDevice = signal<GpsDevice | null>(null);
  gpsHistory?: GpsHistory[];

  gpsEvents: ParsedGpsEvent[] = [];
  eventsColumns: ColumnConfig<ParsedGpsEvent>[] = [
    { key: 'type', label: 'Evento', sortable: true },
    { key: 'dateTime', label: 'Data/Hora', sortable: true },
    { key: 'speed', label: 'Velocidade (km/h)', sortable: true },
    { key: 'description', label: 'Detalhes' },
  ];
  eventsPage = 1;
  eventsLimit = 5;
  eventsTotal = 0;
  eventsTotalPages = 1;

  trips: Trip[] = [];
  tripsColumns: ColumnConfig<any>[] = [
    { key: 'startTime', label: 'Data de Inicio', sortable: true },
    { key: 'startLocation', label: 'Origem' },
    { key: 'endLocation', label: 'Destino' },
    { key: 'driverName', label: 'Motorista' },
  ];
  tripsPage = 1;
  tripsLimit = 5;
  tripsTotal = 0;
  tripsTotalPages = 1;

  expenses: Expense[] = [];
  expensesColumns: ColumnConfig<any>[] = [
    { key: 'date', label: 'Data', sortable: true },
    { key: 'description', label: 'Descrição' },
    { key: 'amount', label: 'Valor', sortable: true },
    { key: 'type', label: 'Categoria' },
  ];
  expensesPage = 1;
  expensesLimit = 5;
  expensesTotal = 0;
  expensesTotalPages = 1;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadGpsDevice(id);
    this.loadGpsEvents(id);
    if (id) this.loadVehicle(id);
    else this.router.navigate(['/veiculos']);
  }
  private parseGpsEvent(rawLog: string, id: number): ParsedGpsEvent {
    const fields = rawLog.split(';');
    if (fields.length < 20) {
      return {
        id,
        type: 'Inválido',
        description: 'Log incompleto',
        header: '',
        dateTime: '',
        speed: 0,
        heading: 0,
        latitude: 0,
        longitude: 0,
        ignition: false,
      };
    }

    const header = fields[0];
    const dateYMD = fields[4]; // "20251103"
    const timeHMS = fields[5]; // "13:22:53"
    const isoDate = `${dateYMD.substring(0, 4)}-${dateYMD.substring(
      4,
      6
    )}-${dateYMD.substring(6, 8)}T${timeHMS}`;

    const latitude = parseFloat(fields[7]) || 0;
    const longitude = parseFloat(fields[8]) || 0;
    const speed = parseFloat(fields[9]) || 0;
    const heading = parseFloat(fields[10]) || 0;
    const ignition = fields[19] === '1'; // Último campo

    const eventMap: Record<string, { type: string; description: string }> = {
      ST300ALT: { type: 'Alerta', description: 'Alarme de sensor' },
      ST300EMG: { type: 'Emergência', description: 'Botão de pânico' },
      ST300HB: { type: 'Heartbeat', description: 'Relatório periódico' },
      ST300GP: { type: 'Posição', description: 'Atualização GPS' },
      ST300IGN: { type: 'Ignição', description: 'Motor ligado/desligado' },
    };

    const info = eventMap[header] || {
      type: 'Desconhecido',
      description: header,
    };

    return {
      id,
      type: info.type,
      description: info.description,
      header,
      dateTime: new Date(isoDate).toLocaleString('pt-BR'),
      latitude,
      longitude,
      speed,
      heading,
      ignition,
    };
  }
  onEventsPageChange(newPage: number) {
    this.eventsPage = newPage;
    if (this.vehicle?.id) this.loadGpsEvents(this.vehicle.id);
  }

  private loadGpsEvents(vehicleId: number) {
    this.serviceGpsDevice
      .getHistoryDevice(vehicleId, this.eventsPage, this.eventsLimit)
      .subscribe({
        next: (res) => {
          console.log(mockHistory, 'req');
          this.gpsHistory = mockHistory;
          this.gpsEvents = mockHistory.map((event) =>
            this.parseGpsEvent(event.rawLog, event.id)
          );
          this.eventsTotal = 0;
          this.eventsPage = 0;
          this.eventsLimit = 0;
          this.eventsTotalPages = 0;
          this.cdr.detectChanges();
        },
        error: () => {
          this.gpsEvents = [];
          this.eventsTotal = 0;
        },
      });
  }

  private loadVehicle(id: number) {
    this.loading = true;
    this.serviceVehicle.getById(id).subscribe({
      next: (res) => {
        this.vehicle = res;
        this.loading = false;
        this.cdr.detectChanges();
        this.loadTopDriver(id);
        this.loadTrips(id);
        this.loadExpenses(id);
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/veiculos']);
      },
    });
  }

  private loadTopDriver(vehicleId: number) {
    this.serviceVehicle.getTopDriverByVehicle(vehicleId).subscribe({
      next: (driver) => {
        this.topDriver = driver;
        this.cdr.detectChanges();
      },
    });
  }

  loadGpsDevice(vehicleId: number) {
    this.serviceGpsDevice.getGpsDeviceByVehicle(vehicleId).subscribe({
      next: (res) => {
        this.gpsDevice.set(res);
        this.markers = [res];
      },
      error: (e) => {
        alertError(
          `Nao foi possivel encontrar dispositivo GPS vinculado a esse veiculo ${e.error.message}`
        );
      },
    });
  }

  // 🔹 Viagens
  private loadTrips(vehicleId: number) {
    this.serviceVehicle
      .getTripsByVehicle(vehicleId, this.tripsPage, this.tripsLimit)
      .subscribe({
        next: (res) => {
          this.trips = res.data;
          this.tripsTotal = res.total;
          this.tripsPage = res.page;
          this.tripsLimit = res.limit;
          this.tripsTotalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: () => {
          this.trips = [];
          this.tripsTotal = 0;
        },
      });
  }

  onTripsPageChange(newPage: number) {
    this.tripsPage = newPage;
    if (this.vehicle?.id) this.loadTrips(this.vehicle.id);
  }

  private loadExpenses(vehicleId: number) {
    this.serviceVehicle
      .getExpensesByVehicle(vehicleId, this.expensesPage, this.expensesLimit)
      .subscribe({
        next: (res) => {
          this.expenses = res.data;
          this.expensesTotal = res.total;
          this.expensesPage = res.page;
          this.expensesLimit = res.limit;
          this.expensesTotalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: (e) => {
          this.expenses = [];
          this.expensesTotal = 0;
        },
      });
  }

  onExpensesPageChange(newPage: number) {
    this.expensesPage = newPage;
    if (this.vehicle?.id) this.loadExpenses(this.vehicle.id);
  }

  onTripSelect(trip: any) {
    this.router.navigate(['/viagens', trip.id]);
  }

  onExpenseSelect(expense: any) {
    this.router.navigate(['/despesas', expense.id]);
  }

  goBack() {
    this.router.navigate(['/veiculos']);
  }

  disableVehicle() {
    if (!this.vehicle?.id) return;
    this.loading = true;

    this.serviceVehicle
      .update(this.vehicle.id, { status: VehicleStatus.INATIVO })
      .subscribe({
        next: () => {
          this.loadVehicle(this.vehicle!.id!);
          this.loading = false;
          alertSuccess('Veículo desabilitado com sucesso.');
        },
        error: (err) => {
          this.loading = false;
          alertError(
            `Erro ao desabilitar o veículo. ${err?.error?.message || ''}`
          );
        },
      });
  }

  activateVehicle() {
    if (!this.vehicle?.id) return;
    this.loading = true;

    this.serviceVehicle
      .update(this.vehicle.id, { status: VehicleStatus.ATIVO })
      .subscribe({
        next: () => {
          this.loadVehicle(this.vehicle!.id!);
          this.loading = false;
          alertSuccess('Veículo reativado com sucesso.');
        },
        error: (err) => {
          this.loading = false;
          alertError(
            `Erro ao reativar o veículo. ${err?.error?.message || ''}`
          );
        },
      });
  }
}
