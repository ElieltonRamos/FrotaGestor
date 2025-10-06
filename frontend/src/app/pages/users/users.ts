import { Component } from '@angular/core';
import { ListUsers } from "./list-users/list-users";
import { CreateUser } from "./create-users/create-users";

@Component({
  selector: 'app-users',
  imports: [ListUsers, CreateUser],
  templateUrl: './users.html',
  styles: ``
})
export class Users {
  activeTab: 'create' | 'list' = 'list';

  selectTab(tab: 'create' | 'list') {
    this.activeTab = tab;
  }
}
