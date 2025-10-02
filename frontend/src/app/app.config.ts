import {
  ApplicationConfig,
  importProvidersFrom,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import {
  provideHttpClient,
  withFetch,
  withInterceptors,
} from '@angular/common/http';

import { routes } from './app.routes';
import { jwtInterceptor } from './guards/jwt-interceptor';

import { NgIconsModule } from '@ng-icons/core';
import { heroTruckSolid, heroUserSolid, heroWrenchSolid, heroCogSolid, heroFireSolid, heroChartBarSolid, heroMapSolid, heroWrenchScrewdriverSolid, heroUserGroupSolid, heroBarsArrowDownSolid, heroEyeSlashSolid, heroEyeSolid, heroLockClosedSolid, heroExclamationCircleSolid} from '@ng-icons/heroicons/solid';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withFetch()),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    importProvidersFrom(
      NgIconsModule.withIcons({
        heroTruckSolid,
        heroUserSolid,
        heroWrenchSolid,
        heroCogSolid,
        heroFireSolid,
        heroMapSolid,
        heroWrenchScrewdriverSolid,
        heroUserGroupSolid,
        heroBarsArrowDownSolid,
        heroEyeSlashSolid,
        heroEyeSolid,
        heroLockClosedSolid,
        heroExclamationCircleSolid,
        heroChartBarSolid,
      })
    ),
  ],
};
