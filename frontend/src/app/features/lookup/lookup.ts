import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { OnboardedUser, OnboardingApiError } from '../../core/models/api.models';
import { OnboardingApiService } from '../../core/services/onboarding-api.service';
import { ToastService } from '../../core/services/toast.service';

type LookupMode = 'email' | 'id';

const UUID_PATTERN = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

@Component({
  selector: 'app-lookup',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './lookup.html',
  styleUrl: './lookup.scss',
})
export class Lookup {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(OnboardingApiService);
  private readonly toast = inject(ToastService);

  protected readonly mode = signal<LookupMode>('email');
  protected readonly loading = signal(false);
  protected readonly foundUser = signal<OnboardedUser | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    id: ['', [Validators.required, Validators.pattern(UUID_PATTERN)]],
  });

  protected setMode(mode: LookupMode): void {
    this.mode.set(mode);
    this.foundUser.set(null);
  }

  protected search(): void {
    const byEmail = this.mode() === 'email';
    const control = byEmail ? this.form.controls.email : this.form.controls.id;

    if (control.invalid) {
      control.markAsTouched();
      return;
    }

    const query = control.value.trim();
    this.loading.set(true);

    const request = byEmail ? this.api.findByEmail(query) : this.api.findById(query);
    request.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (user) => {
        this.foundUser.set(user);
        this.toast.success('User found', `${user.fullName} · ${user.email}`);
      },
      error: (error: OnboardingApiError) => {
        this.foundUser.set(null);
        this.toast.error(
          error.status === 404 ? 'User not found' : 'Lookup failed',
          error.fieldErrors.length
            ? error.fieldErrors.map((item) => item.message).join(' · ')
            : error.message,
        );
      },
    });
  }
}
