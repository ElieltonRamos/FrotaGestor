export interface Driver {
  id?: number;
  nome: string;
  cpf: string;
  cnh: string;
  categoriaCnh?: string;
  validadeCnh?: Date;
  telefone?: string;
  email?: string;
  status: DriverStatus;
}

export type DriverStatus = 'Ativo' | 'Inativo';


