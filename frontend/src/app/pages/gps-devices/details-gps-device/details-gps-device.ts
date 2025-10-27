import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ModalEditComponent } from '../../../components/modal-edit-component/modal-edit-component';
import { DynamicFormComponent, FormField } from '../../../components/dynamic-form/dynamic-form';
import { SelectModalComponent } from '../../../components/select-modal.component/select-modal.component';
import { GpsDeviceService } from '../../../services/gps-device.service';
import { VehicleService } from '../../../services/vehicle.service';
import { GpsDevice } from '../../../interfaces/gpsDevice';
import { Vehicle } from '../../../interfaces/vehicle';
import { ColumnConfig } from '../../../components/base-list-component/base-list-component';
import { FilterConfig } from '../../../components/base-filter-component/base-filter-component';
import { alertError, alertSuccess } from '../../../utils/custom-alerts';

@Component({
  selector: 'app-gps-details',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ModalEditComponent,
    SelectModalComponent,
  ],
  templateUrl: './details-gps-device.html',
})
export class GpsDetails {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private gpsDeviceService = inject(GpsDeviceService);
  private vehicleService = inject(VehicleService);
  private cdr = inject(ChangeDetectorRef);

  gpsDevice: GpsDevice | null = null;
  selectedVehicle: Vehicle | null = null;
  showModal = false;
  showVehicleModal = false;
  vehicleSearchTerm: string = '';
  vehicleInitialFilter: any = {};

  gpsDeviceFields: FormField[] = [
    { name: 'imei', label: 'IMEI', type: 'text', required: true, placeholder: 'Insira o IMEI' },
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
    { key: 'status' as keyof Vehicle, label: 'Status', type: 'status', sortable: true },
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

  loadGpsDevice(id: number) {
    this.gpsDeviceService.getById(id).subscribe({
      next: (device) => {
        this.gpsDevice = device;
        if (device.vehicleId) {
          this.loadVehicle(device.vehicleId);
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        alertError(`Erro ao carregar dispositivo: ${err?.error?.message || 'Erro desconhecido.'}`);
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
        alertError(`Erro ao carregar veículo: ${err?.error?.message || 'Erro desconhecido.'}`);
      },
    });
  }

  onEdit() {
    if (this.gpsDevice) {
      this.showModal = true;
    }
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
        alertError(`Erro ao atualizar dispositivo: ${err?.error?.message || 'Erro desconhecido.'}`);
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

  goBack() {
    this.router.navigate(['/gps-devices']);
  }
}