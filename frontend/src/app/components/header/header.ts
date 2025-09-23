import { Component, inject } from '@angular/core';
import { UserService } from '../../services/user-service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-header',
  imports: [],
  templateUrl: './header.html',
})
export class Header {
  private userService = inject(UserService);
  private router = inject(Router);
  user = '';
  role = '';
  id = 1;

  ngOnInit() {
    const info = this.userService.getUserInfo();
    if (info) {
      this.user = info.username;
      this.role = info.role;
      this.id = info.userId;
    }
  }

  logout() {
    this.userService.logout();
    this.router.navigate(['']);
  }

  navChangePassword() {
    this.router.navigate(['/alterar-senha']);
  }

  navMenu() {
    this.router.navigate(['/menu']);
  }
}
