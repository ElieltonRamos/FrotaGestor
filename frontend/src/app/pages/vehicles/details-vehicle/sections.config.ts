import { ColumnConfig } from '../../../components/base-list-component/base-list-component';

export type DataSetKey = 'gpsEvents' | 'trips' | 'expenses';

export interface SectionConfig {
  key: DataSetKey;
  title: string;
  columns: ColumnConfig<any>[];
}

export const SECTIONS: SectionConfig[] = [
  {
    key: 'trips',
    title: 'Viagens relacionadas',
    columns: [
      { key: 'startTime', label: 'Data de Início', sortable: true },
      { key: 'startLocation', label: 'Origem' },
      { key: 'endLocation', label: 'Destino' },
      { key: 'driverName', label: 'Motorista' },
    ],
  },
  {
    key: 'expenses',
    title: 'Despesas relacionadas',
    columns: [
      { key: 'date', label: 'Data', sortable: true },
      { key: 'description', label: 'Descrição' },
      { key: 'amount', label: 'Valor', sortable: true },
      { key: 'type', label: 'Categoria' },
    ],
  },
  {
    key: 'gpsEvents',
    title: 'Últimos Eventos GPS',
    columns: [
      { key: 'type', label: 'Evento', sortable: true },
      { key: 'dateTime', label: 'Data/Hora', sortable: true },
      { key: 'speed', label: 'Velocidade (km/h)', sortable: true },
      { key: 'description', label: 'Detalhes' },
    ],
  },
];
