import { Component } from '@angular/core';
import { CreateDriverComponent } from './create-driver/create-driver';

@Component({
  selector: 'app-motorista',
  imports: [CreateDriverComponent],
  templateUrl: './motorista.html',
  styles: ``,
})
export class Motorista {
  activeTab: 'create' | 'list' = 'create';

  selectTab(tab: 'create' | 'list') {
    this.activeTab = tab;
  }
}
