import { IB } from 'app/shared/model/b.model';

export interface IA {
  id?: number;
  deparure?: string;
  bs?: IB[] | null;
}

export const defaultValue: Readonly<IA> = {};
