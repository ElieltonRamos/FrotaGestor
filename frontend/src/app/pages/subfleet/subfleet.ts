import { Component } from '@angular/core';
import { CreateSubfleet } from "./create-subfleet/create-subfleet";
import { ListSubfleet } from "./list-subfleet/list-subfleet";

@Component({
  selector: 'app-subfleet',
  imports: [CreateSubfleet, ListSubfleet],
  templateUrl: './subfleet.html',
  styles: ``
})
export class Subfleet {
  activeTab: 'create' | 'list' = 'list';

  selectTab(tab: 'create' | 'list') {
    this.activeTab = tab;
  }
}
