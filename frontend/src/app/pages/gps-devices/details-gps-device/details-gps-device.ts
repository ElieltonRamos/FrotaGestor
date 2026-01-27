import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FormField } from '../../../components/dynamic-form/dynamic-form';
import { SelectModalComponent } from '../../../components/select-modal.component/select-modal.component';
import { GpsDeviceService } from '../../../services/gps-device.service';
import { VehicleService } from '../../../services/vehicle.service';
import { GpsDevice, GpsHistory } from '../../../interfaces/gpsDevice';
import { Vehicle } from '../../../interfaces/vehicle';
import {
  BaseListComponent,
  ColumnConfig,
} from '../../../components/base-list-component/base-list-component';
import {
  FilterConfig,
  BaseFilterComponent,
} from '../../../components/base-filter-component/base-filter-component';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';
import { MapComponent } from '../../../components/map-component/map-component';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../../../interfaces/paginator';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';

@Component({
  selector: 'app-gps-details',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    SelectModalComponent,
    MapComponent,
    BaseListComponent,
    PaginatorComponent,
    BaseFilterComponent,
  ],
  templateUrl: './details-gps-device.html',
})
export class GpsDetails {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private gpsDeviceService = inject(GpsDeviceService);
  private vehicleService = inject(VehicleService);
  private cdr = inject(ChangeDetectorRef);

  markers: GpsDevice[] = [];
  gpsDevice: GpsDevice | null = null;
  selectedVehicle: Vehicle | null = null;
  showModal = false;
  showCustomEditModal = false;
  showVehicleModal = false;
  showVehicleModalForEdit = false;
  showDeleteConfirm = false;
  vehicleSearchTerm: string = '';
  vehicleInitialFilter: any = {};
  editingDevice?: GpsDevice;
  selectedVehicleForEdit?: Vehicle;
  gpsHistoryData: GpsHistory[] = [];
  gpsHistoryTotal = 0;
  gpsHistoryLimit = 10;
  gpsHistoryPage = 1;
  gpsHistoryTotalPages = 1;
  gpsHistorySortKey: keyof GpsHistory = 'dateTime';
  gpsHistorySortAsc = false;
  gpsHistoryFilter: any = {
    startDate: '',
    endDate: '',
  };
  gpsHistoryColumns: ColumnConfig<GpsHistory>[] = [
    {
      key: 'id' as keyof GpsHistory,
      label: 'ID',
      sortable: true,
      type: 'text',
    },
    {
      key: 'dateTime' as keyof GpsHistory,
      label: 'Data/Hora',
      sortable: true,
      type: 'date',
    },
    {
      key: 'speed' as keyof GpsHistory,
      label: 'Velocidade (km/h)',
      sortable: true,
      type: 'text',
    },
    {
      key: 'heading' as keyof GpsHistory,
      label: 'Direção (°)',
      sortable: true,
      type: 'text',
    },
    {
      key: 'ignition' as keyof GpsHistory,
      label: 'Ignição',
      sortable: true,
      type: 'text',
    },
    {
      key: 'satellites' as keyof GpsHistory,
      label: 'Satélites',
      sortable: true,
      type: 'text',
    },
    {
      key: 'gpsFixed' as keyof GpsHistory,
      label: 'GPS Fixo',
      sortable: true,
      type: 'text',
    },
    {
      key: 'gpsQuality' as keyof GpsHistory,
      label: 'Qualidade GPS',
      sortable: true,
      type: 'text',
    },
    {
      key: 'odometer' as keyof GpsHistory,
      label: 'Odômetro',
      sortable: true,
      type: 'text',
    },
    {
      key: 'batteryVoltage' as keyof GpsHistory,
      label: 'Bateria (V)',
      sortable: true,
      type: 'text',
    },
    {
      key: 'messageType' as keyof GpsHistory,
      label: 'Tipo Mensagem',
      sortable: true,
      type: 'text',
    },
    {
      key: 'eventCode' as keyof GpsHistory,
      label: 'Código Evento',
      sortable: true,
      type: 'text',
    },
    {
      key: 'gpsDeviceId' as keyof GpsHistory,
      label: 'ID Dispositivo',
      sortable: true,
      type: 'text',
    },
    {
      key: 'vehicleId' as keyof GpsHistory,
      label: 'ID Veículo',
      sortable: true,
      type: 'text',
    },
  ];

  // 2. Adicionar filtros para o histórico
  gpsHistoryFilters: FilterConfig[] = [
    {
      key: 'startDate',
      label: 'Data Inicial',
      type: 'date',
      placeholder: 'Data inicial...',
    },
    {
      key: 'endDate',
      label: 'Data Final',
      type: 'date',
      placeholder: 'Data final...',
    },
  ];

  // Ícones disponíveis com preview
  availableIcons = [
    { value: 'icon-car.png', label: 'Carro', preview: 'icon-car.png' },
    {
      value: 'icon-truck-box.png',
      label: 'Caminhão Baú',
      preview: 'icon-truck-box.png',
    },
    {
      value: 'icon-motocicle.png',
      label: 'Motocicleta',
      preview: 'icon-motocicle.png',
    },
    { value: 'icon-pickup.png', label: 'Picape', preview: 'icon-pickup.png' },
    {
      value: 'icon-strada-fiat.png',
      label: 'Strada Fiat',
      preview: 'icon-strada-fiat.png',
    },
    { value: 'icon-strada.png', label: 'Strada', preview: 'icon-strada.png' },
  ];

  gpsDeviceFields: FormField[] = [
    {
      name: 'imei',
      label: 'IMEI',
      type: 'text',
      required: true,
      placeholder: 'Insira o IMEI',
    },
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
        'icon-strada.png',
      ],
      required: true,
    },
  ];

  vehicleColumns: ColumnConfig<Vehicle>[] = [
    { key: 'plate' as keyof Vehicle, label: 'Placa', sortable: true },
    { key: 'model' as keyof Vehicle, label: 'Modelo', sortable: true },
    { key: 'brand' as keyof Vehicle, label: 'Marca', sortable: true },
    {
      key: 'status' as keyof Vehicle,
      label: 'Status',
      type: 'status',
      sortable: true,
    },
  ];

  vehicleFilters: FilterConfig[] = [
    { key: 'plate', label: 'Placa', type: 'text', placeholder: 'Placa...' },
    { key: 'model', label: 'Modelo', type: 'text', placeholder: 'Modelo...' },
    { key: 'brand', label: 'Marca', type: 'text', placeholder: 'Marca...' },
    { key: 'status', label: 'Status', type: 'select', options: ['ATIVO'] },
  ];

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadGpsDevice(+id);
    } else {
      alertError('ID do dispositivo não fornecido.');
      this.router.navigate(['/gps-devices']);
    }
  }

  loadGpsHistory(page: number, limit: number) {
    if (!this.gpsDevice?.id) return;

    const startDate = this.gpsHistoryFilter.startDate || undefined;
    const endDate = this.gpsHistoryFilter.endDate || undefined;

    this.gpsDeviceService
      .getHistoryDevice(
        Number(this.gpsDevice.id),
        page,
        limit,
        startDate,
        endDate,
        this.gpsHistorySortAsc,
      )
      .subscribe({
        next: (res) => {
          this.gpsHistoryData = res.data;
          this.gpsHistoryTotal = res.total;
          this.gpsHistoryPage = res.page;
          this.gpsHistoryLimit = res.limit;
          this.gpsHistoryTotalPages = res.totalPages;
          this.cdr.detectChanges();
        },
        error: () => {
          this.gpsHistoryData = [];
          this.gpsHistoryTotal = 0;
          this.gpsHistoryTotalPages = 0;
        },
      });
  }

  applyHistoryFilters() {
    this.gpsHistoryPage = 1;
    this.loadGpsHistory(this.gpsHistoryPage, this.gpsHistoryLimit);
  }

  clearHistoryFilters() {
    this.gpsHistoryFilter = { startDate: '', endDate: '' };
    this.applyHistoryFilters();
  }

  sortHistoryBy(key: keyof GpsHistory) {
    if (this.gpsHistorySortKey === key) {
      this.gpsHistorySortAsc = !this.gpsHistorySortAsc;
    } else {
      this.gpsHistorySortKey = key;
      this.gpsHistorySortAsc = true;
    }
    this.loadGpsHistory(this.gpsHistoryPage, this.gpsHistoryLimit);
  }

  onHistoryPageChange(newPage: number) {
    this.loadGpsHistory(newPage, this.gpsHistoryLimit);
  }

  getBatteryDisplay(): string {
    const voltage = this.gpsDevice?.batteryVoltage;

    if (voltage == null || voltage < 1) {
      return '📴 Desconectada';
    }

    return `🔋 ${voltage}V`;
  }

  loadGpsDevice(id: number) {
    this.gpsDeviceService.getById(id).subscribe({
      next: (device) => {
        this.gpsDevice = device;
        this.markers = [device];
        if (device.vehicleId) {
          this.loadVehicle(device.vehicleId);
        } else {
          this.selectedVehicle = null;
        }
        this.loadGpsHistory(1, 10);
        this.cdr.detectChanges();
      },
      error: (err) => {
        alertError(
          `Erro ao carregar dispositivo: ${err?.error?.message || 'Erro desconhecido.'}`,
        );
        this.router.navigate(['/gps-devices']);
      },
    });
  }

  loadVehicle(vehicleId: number) {
    this.vehicleService.getById(vehicleId).subscribe({
      next: (vehicle) => {
        this.selectedVehicle = vehicle;
        this.cdr.detectChanges();
      },
      error: (err) => {
        alertError(
          `Erro ao carregar veículo: ${err?.error?.message || 'Erro desconhecido.'}`,
        );
      },
    });
  }

  onEdit() {
    if (this.gpsDevice) {
      this.editingDevice = { ...this.gpsDevice };
      this.selectedVehicleForEdit = this.selectedVehicle
        ? { ...this.selectedVehicle }
        : undefined;
      this.showCustomEditModal = true;
    }
  }

  onCloseCustomEditModal() {
    this.showCustomEditModal = false;
    this.editingDevice = undefined;
    this.selectedVehicleForEdit = undefined;
  }

  onSaveCustomEdit() {
    if (!this.editingDevice) return;

    const id = this.editingDevice.id;
    const payload = { ...this.editingDevice };

    if (this.selectedVehicleForEdit) {
      payload.vehicleId = this.selectedVehicleForEdit.id!;
      payload.title = `${this.selectedVehicleForEdit.model} ${this.selectedVehicleForEdit.plate}`;
    }

    delete payload.id;

    this.gpsDeviceService.update(id!, payload).subscribe({
      next: () => {
        this.showCustomEditModal = false;
        this.editingDevice = undefined;
        this.selectedVehicleForEdit = undefined;
        alertSuccess('Dispositivo atualizado com sucesso');
        this.loadGpsDevice(id!);
      },
      error: (err) => {
        alertError(
          `Erro ao atualizar dispositivo: ${err?.error?.message || 'Erro desconhecido.'}`,
        );
      },
    });
  }

  onCloseModal() {
    this.showModal = false;
  }

  onSaveModal(device: GpsDevice) {
    if (!this.gpsDevice || !this.selectedVehicle) {
      alertError('Dispositivo ou veículo não selecionado.');
      return;
    }
    const payload = {
      ...device,
      vehicleId: this.selectedVehicle.id,
      title: `${this.selectedVehicle.model} ${this.selectedVehicle.plate}`,
    };
    this.gpsDeviceService.update(this.gpsDevice.id!, payload).subscribe({
      next: () => {
        alertSuccess('Dispositivo atualizado com sucesso.');
        this.showModal = false;
        this.loadGpsDevice(this.gpsDevice!.id!);
      },
      error: (err) => {
        alertError(
          `Erro ao atualizar dispositivo: ${err?.error?.message || 'Erro desconhecido.'}`,
        );
      },
    });
  }

  onVehicleSelect(vehicle: Vehicle) {
    this.selectedVehicle = vehicle;
    if (this.gpsDevice) {
      this.gpsDevice.vehicleId = vehicle.id!;
    }
    this.showVehicleModal = false;
    this.cdr.detectChanges();
  }

  onVehicleSelectForEdit(vehicle: Vehicle) {
    this.selectedVehicleForEdit = vehicle;
    if (this.editingDevice) {
      this.editingDevice.vehicleId = vehicle.id!;
    }
    this.showVehicleModalForEdit = false;
  }

  unlinkVehicle() {
    if (!this.gpsDevice || !this.gpsDevice.id) {
      alertError('Dispositivo não encontrado.');
      return;
    }

    if (!this.gpsDevice.vehicleId) {
      alertError('Dispositivo já está desvinculado.');
      return;
    }

    const payload = {
      ...this.gpsDevice,
      vehicleId: null,
      title: null,
    };
    delete payload.id;

    this.gpsDeviceService.update(this.gpsDevice.id, payload).subscribe({
      next: () => {
        alertSuccess('Veículo desvinculado com sucesso.');
        this.loadGpsDevice(this.gpsDevice!.id!);
      },
      error: (err) => {
        alertError(
          `Erro ao desvincular veículo: ${err?.error?.message || 'Erro desconhecido.'}`,
        );
      },
    });
  }

  confirmDelete() {
    this.showDeleteConfirm = true;
  }

  cancelDelete() {
    this.showDeleteConfirm = false;
  }

  deleteDevice() {
    if (!this.gpsDevice || !this.gpsDevice.id) {
      alertError('Dispositivo não encontrado.');
      return;
    }

    if (this.gpsDevice.vehicleId) {
      alertError(
        'Não é possível deletar um dispositivo vinculado a um veículo.',
      );
      return;
    }

    this.gpsDeviceService.delete(this.gpsDevice.id).subscribe({
      next: () => {
        alertSuccess('Dispositivo deletado com sucesso.');
        this.router.navigate(['/dispositivos']);
      },
      error: (err) => {
        alertError(
          `Erro ao deletar dispositivo: ${err?.error?.message || 'Erro desconhecido.'}`,
        );
        this.showDeleteConfirm = false;
      },
    });
  }

  vehicleFetcher = (
    page: number,
    limit: number,
    filters: any,
    sortKey: keyof Vehicle,
    sortAsc: boolean,
  ) => {
    return this.vehicleService.getAll(
      page,
      limit,
      filters,
      sortKey as string,
      sortAsc,
    );
  };

  updateVehicleFilter() {
    this.vehicleInitialFilter = {
      ...this.vehicleInitialFilter,
      plate: this.vehicleSearchTerm,
    };
    setTimeout(() => {
      this.vehicleInitialFilter = {
        ...this.vehicleInitialFilter,
        plate: this.vehicleSearchTerm,
      };
    }, 300);
  }

  goBack() {
    this.router.navigate(['/dispositivos']);
  }

  getIconPreview(iconName: string): string {
    const icon = this.availableIcons.find((i) => i.value === iconName);
    return icon?.preview || iconName;
  }

  getIconLabel(iconName: string): string {
    const icon = this.availableIcons.find((i) => i.value === iconName);
    return icon?.label || iconName;
  }

  get canDelete(): boolean {
    return this.gpsDevice ? !this.gpsDevice.vehicleId : false;
  }
}
