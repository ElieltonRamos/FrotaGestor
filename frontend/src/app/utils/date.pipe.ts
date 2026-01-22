import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'brDate',
  standalone: true,
})
export class BrDatePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '';

    let dateStr = typeof value === 'string' ? value.trim() : '';

    // YYYY-MM-DD → Date (meia-noite local)
    if (dateStr.match(/^(\d{4}-\d{2}-\d{2})$/)) {
      dateStr += 'T00:00:00';
    }

    const date = new Date(dateStr);

    if (isNaN(date.getTime())) return '';

    return date.toLocaleDateString('pt-BR'); // 21/01/2026
  }
}
