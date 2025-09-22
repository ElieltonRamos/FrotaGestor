import { Component, inject } from '@angular/core';
import { UserService } from '../../services/user-service';

@Component({
  selector: 'app-header',
  imports: [],
  templateUrl: './header.html',
})
export class Header {
  private userService = inject(UserService);
  user = '';
  role = '';
  id = 1;

  ngOnInit() {
    const info = this.userService.getUserInfo();
    if (info) {
      this.user = info.username;
      this.role = info.role;
      this.id = info.id;
    }
  }
}
