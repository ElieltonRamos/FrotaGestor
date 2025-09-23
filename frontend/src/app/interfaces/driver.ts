export interface Driver {
  id?: number;
  name: string;
  cpf: string;
  cnh: string;
  cnhCategory?: string;
  cnhExpiration?: Date;
  phone?: string;
  email?: string;
  status: DriverStatus;
  deletedAt?: Date;
}

export type DriverStatus = 'ATIVO' | 'INATIVO';


