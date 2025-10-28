// guards/guest-guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserService } from '../services/user.service';

export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  const userService = inject(UserService);

  if (userService.isAuthenticated()) {
    router.navigate(['/menu']);
    return false;
  }

  return true;
};
