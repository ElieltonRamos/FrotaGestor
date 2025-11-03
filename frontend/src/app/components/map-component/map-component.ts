import {
  Component,
  AfterViewInit,
  OnDestroy,
  Input,
  OnChanges,
  SimpleChanges,
  inject,
  NgZone,
} from '@angular/core';
import { Router } from '@angular/router';
import {
  Map,
  NavigationControl,
  Marker,
  LngLatBounds,
  Popup,
  LngLatLike,
} from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { GpsDevice, GpsHistory } from '../../interfaces/gpsDevice';

declare global {
  interface Window {
    angularComponentRef?: {
      zone: any;
      component: any;
    };
  }
}

@Component({
  selector: 'app-map',
  templateUrl: './map-component.html',
  styles: [
    `
      #map {
        height: 400px;
        width: 100%;
      }
    `,
  ],
})
export class MapComponent implements AfterViewInit, OnDestroy, OnChanges {
  private map!: Map;
  private gpsMarkers: Marker[] = [];
  private historyMarkers: Marker[] = [];
  private userMarker: Marker | null = null;
  private fallbackCoords: [number, number] = [-42.840379, -14.948981];
  private userCoords: [number, number] = this.fallbackCoords;
  private ngZone = inject(NgZone);
  private router = inject(Router);

  @Input() markers: GpsDevice[] = [];
  @Input() historyPoints: GpsHistory[] = [];
  @Input() vehicleIconUrl?: string;
  @Input() showUserMarker: boolean = true;
  @Input() mapMode: 'vehicles' | 'history' = 'vehicles';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['markers'] && this.map && this.mapMode === 'vehicles') {
      this.updateMarkers();
    }

    if (changes['historyPoints'] && this.map && this.mapMode === 'history') {
      this.updateHistoryPath();
    }

    if (changes['mapMode'] && this.map) {
      this.handleModeChange();
    }
  }

  async ngAfterViewInit(): Promise<void> {
    // this.userCoords = await this.getUserLocation();
    window.angularComponentRef = {
      zone: this.ngZone,
      component: this,
    };

    this.map = new Map({
      container: 'map',
      style:
        'https://api.maptiler.com/maps/satellite/style.json?key=fmMkq3Snb5WQdJPLGHIW',
      center: this.userCoords,
      zoom: 14,
    });

    this.map.addControl(new NavigationControl(), 'top-right');

    // Adicionar marcador do usuário se necessário
    if (this.showUserMarker) {
      this.addUserMarker(this.userCoords);
    }

    // Aguardar o mapa carregar antes de adicionar elementos
    this.map.on('load', () => {
      if (this.mapMode === 'vehicles' && this.markers.length > 0) {
        this.updateMarkers();
      } else if (this.mapMode === 'history' && this.historyPoints.length > 0) {
        this.updateHistoryPath();
      }
    });

    // Adicionar botão de recentralizar
    this.addRecenterButton();
  }

  private handleModeChange(): void {
    // Limpar marcadores do modo anterior
    if (this.mapMode === 'vehicles') {
      this.clearHistoryPath();
      this.updateMarkers();
    } else {
      this.clearVehicleMarkers();
      this.updateHistoryPath();
    }
  }

  private updateMarkers(): void {
    this.clearVehicleMarkers();

    const bounds = new LngLatBounds();
    let hasValidMarkers = false;

    this.markers.forEach((m) => {
      if (m.latitude && m.longitude) {
        const marker = this.addMarker(m);
        this.gpsMarkers.push(marker);
        bounds.extend([m.longitude, m.latitude]);
        hasValidMarkers = true;
      } else {
        console.warn('Invalid marker coordinates:', m);
      }
    });

    // Incluir coordenadas do usuário nos limites se o marcador estiver visível
    if (this.showUserMarker && this.userMarker) {
      bounds.extend(this.userCoords);
    }

    // Ajustar o mapa para os limites
    if (hasValidMarkers || (this.showUserMarker && this.userMarker)) {
      this.ngZone.run(() => {
        this.map.fitBounds(bounds, { padding: 60, maxZoom: 14 });
      });
    }
  }

  private updateHistoryPath(): void {
    this.clearHistoryPath();

    if (this.historyPoints.length === 0) return;

    const bounds = new LngLatBounds();

    // Criar linha do trajeto conectando todos os pontos
    const coordinates: [number, number][] = this.historyPoints.map((point) => {
      bounds.extend([point.longitude, point.latitude]);
      return [point.longitude, point.latitude];
    });

    // Adicionar source e layer para a linha principal
    if (this.map.getSource('route')) {
      (this.map.getSource('route') as any).setData({
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'LineString',
          coordinates: coordinates,
        },
      });
    } else {
      this.map.addSource('route', {
        type: 'geojson',
        data: {
          type: 'Feature',
          properties: {},
          geometry: {
            type: 'LineString',
            coordinates: coordinates,
          },
        },
      });

      this.map.addLayer({
        id: 'route',
        type: 'line',
        source: 'route',
        layout: {
          'line-join': 'round',
          'line-cap': 'round',
        },
        paint: {
          'line-color': '#3B82F6',
          'line-width': 4,
          'line-opacity': 0.75,
        },
      });

      // Adicionar linha de borda/sombra para melhor visualização
      this.map.addLayer({
        id: 'route-outline',
        type: 'line',
        source: 'route',
        layout: {
          'line-join': 'round',
          'line-cap': 'round',
        },
        paint: {
          'line-color': '#1E3A8A',
          'line-width': 6,
          'line-opacity': 0.3,
        },
      }, 'route');
    }

    // Adicionar linhas pontilhadas entre pontos intermediários para melhor visualização
    if (this.historyPoints.length > 2) {
      const segmentFeatures: any[] = [];
      for (let i = 0; i < this.historyPoints.length - 1; i++) {
        segmentFeatures.push({
          type: 'Feature' as const,
          properties: { segment: i },
          geometry: {
            type: 'LineString' as const,
            coordinates: [
              [this.historyPoints[i].longitude, this.historyPoints[i].latitude],
              [this.historyPoints[i + 1].longitude, this.historyPoints[i + 1].latitude],
            ],
          },
        });
      }

      if (!this.map.getSource('route-segments')) {
        this.map.addSource('route-segments', {
          type: 'geojson',
          data: {
            type: 'FeatureCollection' as const,
            features: segmentFeatures,
          } as any,
        });

        this.map.addLayer({
          id: 'route-segments',
          type: 'line',
          source: 'route-segments',
          layout: {
            'line-join': 'round',
            'line-cap': 'round',
          },
          paint: {
            'line-color': '#60A5FA',
            'line-width': 2,
            'line-dasharray': [2, 2],
            'line-opacity': 0.5,
          },
        });
      }
    }

    // Adicionar marcadores de início e fim
    const firstPoint = this.historyPoints[0];
    const lastPoint = this.historyPoints[this.historyPoints.length - 1];

    // Marcador de início (verde)
    this.addHistoryMarker(firstPoint, 'start');

    // Marcador de fim (ícone do veículo)
    const iconUrl = this.vehicleIconUrl;
    this.addHistoryMarker(lastPoint, 'end', iconUrl);

    // Adicionar marcadores intermediários a cada N pontos
    const interval = Math.max(1, Math.floor(this.historyPoints.length / 10));
    this.historyPoints.forEach((point, index) => {
      if (index > 0 && index < this.historyPoints.length - 1 && index % interval === 0) {
        this.addHistoryMarker(point, 'intermediate');
      }
    });

    // Ajustar mapa aos limites do trajeto
    this.ngZone.run(() => {
      this.map.fitBounds(bounds, { padding: 60, maxZoom: 16 });
    });
  }

  private addHistoryMarker(
    point: GpsHistory,
    type: 'start' | 'end' | 'intermediate',
    iconUrl?: string
  ): void {
    const el = document.createElement('div');
    el.className = 'history-marker';

    if (type === 'end' && iconUrl) {
      // Usar ícone do veículo para o ponto final
      el.style.backgroundImage = `url(${iconUrl})`;
      el.style.backgroundSize = 'cover';
      el.style.backgroundPosition = 'center';
      el.style.width = '40px';
      el.style.height = '40px';
      el.style.borderRadius = '50%';
      el.style.boxShadow = '0 4px 8px rgba(0,0,0,0.4)';
    } else {
      // Estilo baseado no tipo de marcador
      const colors = {
        start: '#10B981', // Verde
        end: '#EF4444', // Vermelho (fallback se não houver ícone)
        intermediate: '#3B82F6', // Azul
      };

      const sizes = {
        start: '28px',
        end: '28px',
        intermediate: '14px',
      };

      el.style.backgroundColor = colors[type];
      el.style.width = sizes[type];
      el.style.height = sizes[type];
      el.style.borderRadius = '50%';
      el.style.border = type === 'intermediate' ? '2px solid white' : '3px solid white';
      el.style.boxShadow = '0 2px 6px rgba(0,0,0,0.3)';
    }

    el.style.cursor = 'pointer';

    const timestamp = new Date(point.dateTime).toLocaleString('pt-BR');
    const speedText = '0 km/h';

    const labels = {
      start: '🚩 Início do Trajeto',
      end: '🏁 Posição Atual',
      intermediate: '📍 Ponto Intermediário',
    };

    const popupHtml = `
      <div class="bg-gray-100 p-4 rounded-xl shadow-lg max-w-xs font-sans">
        <h3 class="text-base font-bold text-gray-800 mb-2">${labels[type]}</h3>
        <div class="text-sm text-gray-600 space-y-1">
          <p><strong>Data/Hora:</strong> ${timestamp}</p>
        </div>
        <a href="https://www.google.com/maps/search/?api=1&query=${point.latitude},${point.longitude}"
           target="_blank"
           class="block text-center w-full bg-blue-500 text-white py-2 rounded-md font-semibold text-sm hover:bg-blue-600 transition-colors duration-200 mt-3">
          Abrir no Google Maps
        </a>
      </div>
    `;

    const popup = new Popup({ offset: type === 'end' && iconUrl ? 25 : 15 }).setHTML(popupHtml);

    const marker = new Marker({ element: el })
      .setLngLat([point.longitude, point.latitude])
      .setPopup(popup)
      .addTo(this.map);

    this.historyMarkers.push(marker);
  }

  private clearVehicleMarkers(): void {
    this.gpsMarkers.forEach((marker) => marker.remove());
    this.gpsMarkers = [];
  }

  private clearHistoryPath(): void {
    // Remover marcadores de histórico
    this.historyMarkers.forEach((marker) => marker.remove());
    this.historyMarkers = [];

    // Remover linhas do trajeto
    if (this.map.getLayer('route-segments')) {
      this.map.removeLayer('route-segments');
    }
    if (this.map.getLayer('route')) {
      this.map.removeLayer('route');
    }
    if (this.map.getLayer('route-outline')) {
      this.map.removeLayer('route-outline');
    }
    if (this.map.getSource('route-segments')) {
      this.map.removeSource('route-segments');
    }
    if (this.map.getSource('route')) {
      this.map.removeSource('route');
    }
  }

  private addRecenterButton(): void {
    const img = document.createElement('img');
    img.src = 'btn-recenter-map.png';
    img.alt = 'Recentralizar mapa';
    img.title = 'Recentralizar';
    img.className =
      'absolute top-4 left-4 z-50 cursor-pointer shadow-lg transition transform hover:scale-105 bg-gray-100 rounded-full';
    img.style.width = '40px';

    img.onclick = () => {
      this.ngZone.run(() => {
        if (this.mapMode === 'vehicles' && this.showUserMarker) {
          this.map.flyTo({ center: this.userCoords, zoom: 14 });
        } else if (this.mapMode === 'history' && this.historyPoints.length > 0) {
          const bounds = new LngLatBounds();
          this.historyPoints.forEach((point) => {
            bounds.extend([point.longitude, point.latitude]);
          });
          this.map.fitBounds(bounds, { padding: 60, maxZoom: 16 });
        }
      });
    };

    const container = document.getElementById('map');
    if (container) {
      container.appendChild(img);
      container.style.position = 'relative';
    }
  }

  private addUserMarker(coords: [number, number]): void {
    const el = document.createElement('div');
    el.className = 'user-marker';
    el.style.width = '32px';
    el.style.height = '32px';
    el.style.position = 'relative';
    el.style.cursor = 'pointer';

    const img = document.createElement('img');
    img.src = 'user-marker.png';
    img.style.width = '100%';
    img.style.height = '100%';
    img.style.borderRadius = '50%';
    img.style.display = 'block';
    img.style.filter = 'drop-shadow(0 0 6px rgba(255,255,255,0.9))';

    el.appendChild(img);

    const popup = new Popup({ offset: 25 }).setHTML(`
      <div class="bg-gray-100 p-4 rounded-xl shadow-lg max-w-xs font-sans">
        <h3 class="text-base font-bold text-gray-800 mb-2">Você está aqui</h3>
      </div>
    `);

    this.userMarker = new Marker({ element: el })
      .setLngLat(coords)
      .setPopup(popup)
      .addTo(this.map);
  }

  private addMarker(markerData: GpsDevice): Marker {
    const el = document.createElement('div');
    el.className = 'custom-marker';

    if (markerData.iconMapUrl) {
      el.style.backgroundImage = `url(${markerData.iconMapUrl})`;
      el.style.backgroundSize = 'cover';
      el.style.width = '32px';
      el.style.height = '32px';
      el.style.borderRadius = '50%';
    } else {
      el.style.backgroundColor = '#EF4444';
      el.style.width = '16px';
      el.style.height = '16px';
      el.style.borderRadius = '50%';
      el.style.border = '2px solid white';
    }

    const popupHtml = `
      <div class="bg-gray-100 p-4 rounded-xl shadow-lg max-w-xs font-sans">
        <h3 class="text-base font-bold text-gray-800 mb-2">${
          markerData.title || 'Ponto'
        }</h3>
        <p class="text-sm text-gray-600 mb-3">${
          markerData.title || 'Sem descrição disponível.'
        }</p>
        <div class="flex flex-col gap-2">
          <a href="https://www.google.com/maps/search/?api=1&query=${
            markerData.latitude
          },${markerData.longitude}"
             target="_blank"
             class="block text-center w-full bg-blue-500 text-white py-2 rounded-md font-semibold text-sm hover:bg-blue-600 transition-colors duration-200">
            Abrir no Google Maps
          </a>
          ${
            markerData.vehicleId
              ? `<button
                   onclick="window.angularComponentRef.zone.run(() => window.angularComponentRef.component.navigateToVehicle(${markerData.vehicleId}))"
                   class="block text-center w-full bg-blue-500 text-white py-2 rounded-md font-semibold text-sm hover:bg-blue-600 transition-colors duration-200">
                   Detalhes do Veículo
                 </button>`
              : ''
          }
        </div>
      </div>
    `;
    const popup = new Popup({ offset: 25 }).setHTML(popupHtml);

    const marker = new Marker({ element: el })
      .setLngLat([markerData.longitude ?? 0, markerData.latitude ?? 0])
      .setPopup(popup)
      .addTo(this.map);

    return marker;
  }

  private getUserLocation(): Promise<[number, number]> {
    return new Promise((resolve) => {
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (pos) => resolve([pos.coords.longitude, pos.coords.latitude]),
          () => resolve(this.fallbackCoords),
          { enableHighAccuracy: true, timeout: 5000 }
        );
      } else {
        resolve(this.fallbackCoords);
      }
    });
  }

  ngOnDestroy(): void {
    delete window.angularComponentRef;
    if (this.userMarker) {
      this.userMarker.remove();
    }
    this.clearVehicleMarkers();
    this.clearHistoryPath();
    this.map.remove();
  }

  navigateToVehicle(vehicleId: number) {
    this.ngZone.run(() => {
      this.router.navigate(['/veiculos', vehicleId]);
    });
  }
}