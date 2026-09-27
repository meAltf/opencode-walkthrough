import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'onboarding' },
  {
    path: 'onboarding',
    title: 'Onboarding · User Onboarding API',
    loadComponent: () => import('./features/onboarding/onboarding').then((m) => m.Onboarding),
  },
  {
    path: 'lookup',
    title: 'Lookup · User Onboarding API',
    loadComponent: () => import('./features/lookup/lookup').then((m) => m.Lookup),
  },
  { path: '**', redirectTo: 'onboarding' },
];
