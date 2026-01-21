import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'brDate',
  standalone: true  // ✅ Para Angular 17+ standalone
})
export class BrDatePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '';
    
    // Converte "2026-01-22" → Date válido
    const date = new Date(value + 'T12:00:00');  // +T12 evita timezone issues
    
    return date.toLocaleDateString('pt-BR');  // "22/01/2026"
  }
}
