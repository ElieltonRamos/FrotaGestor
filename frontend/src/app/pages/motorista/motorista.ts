import { Component } from '@angular/core';
import { CreateDriverComponent } from './create-driver/create-driver';
import { ListDriver } from './list-driver/list-driver';

@Component({
  selector: 'app-motorista',
  imports: [CreateDriverComponent, ListDriver],
  templateUrl: './motorista.html',
  styles: ``,
})
export class Motorista {
  activeTab: 'create' | 'list' = 'list';

  selectTab(tab: 'create' | 'list') {
    this.activeTab = tab;
  }
}
