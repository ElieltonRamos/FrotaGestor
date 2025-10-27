import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  BaseListComponent,
  ColumnConfig,
} from '../../components/base-list-component/base-list-component';
import { PaginatorComponent } from '../../components/paginator/paginator.component';
import {
  BaseFilterComponent,
  FilterConfig,
} from '../../components/base-filter-component/base-filter-component';
import { ModalEditComponent } from '../../components/modal-edit-component/modal-edit-component';
import {
  DynamicFormComponent,
  FormField,
} from '../../components/dynamic-form/dynamic-form';
import { GpsDeviceService } from '../../services/gps-device.service';
import { Router } from '@angular/router';
import { GpsDevice } from '../../interfaces/gpsDevice';
import { PaginatedResponse } from '../../interfaces/paginator';
import { alertError, alertSuccess } from '../../utils/custom-alerts';
import { Vehicle } from '../../interfaces/vehicle';
import { VehicleService } from '../../services/vehicle.service';
import { SelectModalComponent } from '../../components/select-modal.component/select-modal.component';

@Component({
  selector: 'app-list-gps-devices',
  imports: [
    FormsModule,
    CommonModule,
    BaseListComponent,
    PaginatorComponent,
    BaseFilterComponent,
    ModalEditComponent,
    DynamicFormComponent,
    SelectModalComponent,
  ],
  templateUrl: './gps-devices.html',
})
export class GpsDevices {
  private service = inject(GpsDeviceService);
  private vehicleService = inject(VehicleService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  gpsDeviceFields: FormField[] = [
    { name: 'imei', label: 'IMEI', type: 'text', required: true, placeholder: 'insira o imei' },
    {
      placeholder: 'Ícone no Mapa',
      name: 'iconMapUrl',
      label: 'Ícone no Mapa',
      type: 'select',
      options: [
        'icon-car.png',
        'icon-truck-box.png',
        'icon-motocicle.png',
        'icon-pickup.png',
        'icon-strada-fiat.png',
        'icon-strada.png'
      ],
      required: true,
    },
  ];

  gpsDeviceColumns: ColumnConfig<GpsDevice>[] = [
    { key: 'imei' as keyof GpsDevice, label: 'IMEI', sortable: true },
    { key: 'vehicleId' as keyof GpsDevice, label: 'Veículo', sortable: true },
    { key: 'latitude' as keyof GpsDevice, label: 'Latitude', sortable: true },
    { key: 'longitude' as keyof GpsDevice, label: 'Longitude', sortable: true },
    { key: 'dateTime' as keyof GpsDevice, label: 'Data/Hora', sortable: true, type: 'date' },
    { key: 'speed' as keyof GpsDevice, label: 'Velocidade', sortable: true },
    { key: 'heading' as keyof GpsDevice, label: 'Direção', sortable: true },
    { key: 'ignition' as keyof GpsDevice, label: 'Ignição', type: 'text', sortable: true },
  ];

  gpsDeviceFilters = [
    { key: 'imei', label: 'IMEI', type: 'text', placeholder: 'IMEI...' },
    { key: 'vehicleId', label: 'Veículo', type: 'text', placeholder: 'Veículo...' },
    { key: 'dateTime', label: 'Data/Hora', type: 'date', placeholder: 'Data...' },
  ] satisfies FilterConfig[];

  vehicleColumns: ColumnConfig<Vehicle>[] = [
    { key: 'plate' as keyof Vehicle, label: 'Placa', sortable: true },
    { key: 'model' as keyof Vehicle, label: 'Modelo', sortable: true },
    { key: 'brand' as keyof Vehicle, label: 'Marca', sortable: true },
    { key: 'status' as keyof Vehicle, label: 'Status', type: 'status', sortable: true },
  ];

  vehicleFilters = [
    { key: 'plate', label: 'Placa', type: 'text', placeholder: 'Placa...' },
    { key: 'model', label: 'Modelo', type: 'text', placeholder: 'Modelo...' },
    { key: 'brand', label: 'Marca', type: 'text', placeholder: 'Marca...' },
    { key: 'status', label: 'Status', type: 'select', options: ['ATIVO'] },
  ] satisfies FilterConfig[];

  gpsDevices: GpsDevice[] = [];
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 1;
  selectedDevice?: GpsDevice;
  showModal = false;
  showVehicleModal = false;
  selectedVehicle?: Vehicle;
  vehicleSearchTerm: string = '';
  vehicleInitialFilter: any = {};

  // Added missing properties
  sortKey: keyof GpsDevice = 'imei';
  sortAsc: boolean = true;
  filter = {
    imei: '',
    vehicleId: '',
    dateTime: '',
  };

  ngOnInit() {
    this.listDevices(1, 10);
  }

  listDevices(page: number, limit: number) {
    this.service.getAll(page, limit, this.filter, this.sortKey, this.sortAsc).subscribe({
      next: (res) => {
        this.gpsDevices = res.data;
        this.total = res.total;
        this.page = res.page;
        this.limit = res.limit;
        this.totalPages = res.totalPages;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao carregar dispositivos:', err);
        this.gpsDevices = [];
        this.total = 0;
        this.totalPages = 0;
      },
    });
  }

  applyFilters() {
    this.page = 1;
    this.listDevices(this.page, this.limit);
    this.cdr.detectChanges();
  }

  sortBy(key: keyof GpsDevice) {
    if (this.sortKey === key) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortKey = key;
      this.sortAsc = true;
    }
    this.listDevices(this.page, this.limit);
  }

  clearFilters() {
    this.filter = { imei: '', vehicleId: '', dateTime: '' };
    this.applyFilters();
  }

  onPageChange(newPage: number) {
    this.listDevices(newPage, this.limit);
  }

  onEdit(device: GpsDevice) {
    this.selectedDevice = { ...device };
    this.showModal = true;
  }

  onCloseModal() {
    this.showModal = false;
  }

  onSaveModal(device: GpsDevice) {
    const id = device.id;
    delete device.id;

    const method = id ? this.service.update(id, device) : this.service.create(device);
    method.subscribe({
      next: () => {
        this.listDevices(1, 10);
        this.showModal = false;
        this.selectedDevice = undefined;
        alertSuccess(id ? 'Dispositivo atualizado com sucesso' : 'Dispositivo cadastrado com sucesso');
      },
      error: (err) => {
        alertError(`Ocorreu um erro ao salvar o dispositivo. ${err?.error?.message || 'Erro desconhecido.'}`);
      },
    });
  }

  onNavDetails(id?: number) {
    if (!id) return;
    this.router.navigate(['/gps-devices', id]);
  }

  saveDevice(device: GpsDevice) {
    console.log(device)
    if (!this.selectedVehicle) {
      alertError('Selecione um veículo antes de salvar');
      return;
    }
    const title = `${this.selectedVehicle.model} ${this.selectedVehicle.plate}`
    const payload = { ...device, vehicleId: this.selectedVehicle.id, title };
    this.service.create(payload).subscribe({
      next: () => {
        alertSuccess('Dispositivo cadastrado com sucesso');
        this.listDevices(1, 10);
        this.selectedVehicle = undefined; // Reset after save
      },
      error: (err) => {
        alertError(`Erro ao cadastrar dispositivo: ${err?.error?.message || 'Erro desconhecido.'}`);
      },
    });
  }

  onVehicleSelect(vehicle: Vehicle) {
    this.selectedVehicle = vehicle;
    // Update the vehicleId field in the form or selectedDevice if in edit mode
    if (this.selectedDevice) {
      this.selectedDevice.vehicleId = vehicle.id!;
    } else {
      // Assume dynamic-form updates the vehicleId field indirectly via form state
    }
    this.showVehicleModal = false;
  }

  vehicleFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Vehicle,
    sortAsc: boolean
  ) => {
    return this.vehicleService.getAll(page, limit, filters, sortKey as string, sortAsc);
  };

  updateVehicleFilter() {
    this.vehicleInitialFilter = { ...this.vehicleInitialFilter, plate: this.vehicleSearchTerm };
    setTimeout(() => {
      this.vehicleInitialFilter = { ...this.vehicleInitialFilter, plate: this.vehicleSearchTerm };
    }, 300);
  }
}