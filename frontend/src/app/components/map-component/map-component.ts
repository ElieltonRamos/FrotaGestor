import { Component, AfterViewInit, OnDestroy, Input } from '@angular/core';
import {
  Map,
  NavigationControl,
  Marker,
  LngLatBounds,
  Popup,
} from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';

export interface CustomMarker {
  lat: number;
  lng: number;
  color?: string;
  title?: string;
  description?: string;
  iconUrl?: string;
}

@Component({
  selector: 'app-map',
  templateUrl: './map-component.html',
})
export class MapComponent implements AfterViewInit, OnDestroy {
  private map!: Map;
  private fallbackCoords: [number, number] = [-42.840379, -14.948981];
  private userCoords: [number, number] = this.fallbackCoords;

  @Input() markers: CustomMarker[] = [];

  async ngAfterViewInit(): Promise<void> {
    this.userCoords = await this.getUserLocation();

    this.map = new Map({
      container: 'map',
      style:
        'https://api.maptiler.com/maps/satellite/style.json?key=fmMkq3Snb5WQdJPLGHIW',
      center: this.userCoords,
      zoom: 14,
    });

    this.map.addControl(new NavigationControl(), 'top-right');

    this.addUserMarker(this.userCoords);

    if (this.markers.length > 0) {
      const bounds = new LngLatBounds();
      this.markers.forEach((m) => {
        this.addMarker(m);
        bounds.extend([m.lng, m.lat]);
      });
      bounds.extend(this.userCoords);
      this.map.fitBounds(bounds, { padding: 60, maxZoom: 14 });
    }

    // Adiciona botão de recentralizar
    this.addRecenterButton();
  }

  /** Botão de recentralizar mapa na posição do usuário */
  private addRecenterButton(): void {
    const button = document.createElement('button');
    button.innerHTML = '📍';
    button.title = 'Recentralizar';
    button.className =
      'absolute top-4 left-4 z-50 bg-white p-1 rounded-full shadow-lg hover:bg-blue-500 hover:text-white transition';

    button.onclick = () => {
      this.map.flyTo({ center: this.userCoords, zoom: 14 });
    };

    // Insere no container do mapa
    const container = document.getElementById('map');
    if (container) {
      container.appendChild(button);
      container.style.position = 'relative'; // garante que o botão fique sobre o mapa
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

    new Marker({ element: el })
      .setLngLat(coords)
      .setPopup(popup)
      .addTo(this.map);
  }

  private addMarker(markerData: CustomMarker): void {
    const el = document.createElement('div');
    el.className = 'custom-marker';

    if (markerData.iconUrl) {
      el.style.backgroundImage = `url(${markerData.iconUrl})`;
      el.style.backgroundSize = 'cover';
      el.style.width = '32px';
      el.style.height = '32px';
      el.style.borderRadius = '50%';
    } else {
      el.style.backgroundColor = markerData.color || '#EF4444';
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
          markerData.description || 'Sem descrição disponível.'
        }</p>
        <a href="https://www.google.com/maps/search/?api=1&query=${
          markerData.lat
        },${markerData.lng}"
           target="_blank"
           class="block text-center w-full bg-blue-500 text-white py-2 rounded-md font-semibold text-sm hover:bg-blue-600 transition-colors duration-200">
          Abrir no Google Maps
        </a>
      </div>
    `;
    const popup = new Popup({ offset: 25 }).setHTML(popupHtml);

    new Marker({ element: el })
      .setLngLat([markerData.lng, markerData.lat])
      .setPopup(popup)
      .addTo(this.map);
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
    this.map.remove();
  }
}
