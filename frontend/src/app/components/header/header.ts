import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../../services/user.service';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { NgIcon } from "@ng-icons/core";
import { VERSION } from '../../services/api.url';

@Component({
  selector: 'app-header',
  imports: [CommonModule, NgIcon],
  templateUrl: './header.html',
})
export class Header {
  private userService = inject(UserService);
  private router = inject(Router);
  private location = inject(Location);

  user = '';
  role = '';
  id = 1;
  version = VERSION

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

  goBackHistory() {
    this.location.back();
  }

  goForwardHistory() {
    this.location.forward();
  }
}