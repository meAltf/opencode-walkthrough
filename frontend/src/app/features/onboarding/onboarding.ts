import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { FieldError, OnboardedUser, OnboardingApiError } from '../../core/models/api.models';
import { OnboardingApiService } from '../../core/services/onboarding-api.service';
import { ToastService } from '../../core/services/toast.service';

const notInFuture: ValidatorFn = (control: AbstractControl) => {
  const value = control.value as string | null;
  if (!value) {
    return null;
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return { invalidDate: true };
  }
  return parsed.getTime() > Date.now() ? { futureDate: true } : null;
};

type ControlName = 'email' | 'fullName' | 'password' | 'dateOfBirth' | 'termsAccepted';

@Component({
  selector: 'app-onboarding',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './onboarding.html',
  styleUrl: './onboarding.scss',
})
export class Onboarding {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(OnboardingApiService);
  private readonly toast = inject(ToastService);

  protected readonly submitting = signal(false);
  protected readonly createdUser = signal<OnboardedUser | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});
  protected readonly passwordVisible = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    dateOfBirth: ['', [Validators.required, notInFuture]],
    termsAccepted: [false, [Validators.requiredTrue]],
  });

  private readonly passwordValue = toSignal(this.form.controls.password.valueChanges, {
    initialValue: '',
  });

  protected readonly passwordStrength = computed(() => {
    const value = this.passwordValue();
    let score = 0;
    if (value.length >= 8) score++;
    if (value.length >= 12) score++;
    if (/[A-Z]/.test(value) && /[a-z]/.test(value)) score++;
    if (/\d/.test(value)) score++;
    if (/[^A-Za-z0-9]/.test(value)) score++;
    return { score, label: ['Too short', 'Weak', 'Fair', 'Good', 'Strong', 'Excellent'][score] };
  });

  protected control(name: ControlName): AbstractControl {
    return this.form.controls[name];
  }

  protected showError(name: ControlName): boolean {
    const control = this.control(name);
    return control.touched && control.invalid;
  }

  protected errorText(name: ControlName): string {
    const server = this.serverErrors()[name];
    if (server) {
      return server;
    }
    const errors = this.control(name).errors ?? {};
    if (errors['required']) {
      return name === 'termsAccepted' ? 'You must accept the terms to continue.' : 'This field is required.';
    }
    if (errors['requiredTrue']) {
      return 'You must accept the terms to continue.';
    }
    if (errors['email']) {
      return 'Enter a valid email address.';
    }
    if (errors['minlength']) {
      const required = errors['minlength'].requiredLength as number;
      return `Must be at least ${required} characters.`;
    }
    if (errors['maxlength']) {
      const allowed = errors['maxlength'].requiredLength as number;
      return `Must be at most ${allowed} characters.`;
    }
    if (errors['futureDate']) {
      return 'Date of birth must be in the past.';
    }
    if (errors['invalidDate']) {
      return 'Enter a valid date.';
    }
    return 'Invalid value.';
  }

  protected togglePasswordVisibility(): void {
    this.passwordVisible.update((visible) => !visible);
  }

  protected resetForm(): void {
    this.form.reset({ email: '', fullName: '', password: '', dateOfBirth: '', termsAccepted: false });
    this.serverErrors.set({});
    this.createdUser.set(null);
  }

  protected submit(): void {
    this.serverErrors.set({});

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.error('Check the form', 'Some fields need your attention before we can continue.');
      return;
    }

    const value = this.form.getRawValue();
    this.submitting.set(true);

    this.api
      .onboard({
        email: value.email.trim(),
        fullName: value.fullName.trim(),
        password: value.password,
        dateOfBirth: value.dateOfBirth,
        termsAccepted: value.termsAccepted,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (user) => {
          this.createdUser.set(user);
          this.resetForm();
          this.toast.success('Onboarding complete', `${user.fullName} is now onboarded.`);
        },
        error: (error: OnboardingApiError) => this.reportError(error),
      });
  }

  private reportError(error: OnboardingApiError): void {
    const mapped: Record<string, string> = {};
    for (const fieldError of error.fieldErrors as FieldError[]) {
      mapped[fieldError.field] = fieldError.message;
    }
    this.serverErrors.set(mapped);

    const detail = error.fieldErrors.length
      ? error.fieldErrors.map((item) => `${item.field}: ${item.message}`).join(' · ')
      : error.message;
    this.toast.error(titleFor(error), detail);
  }
}

function titleFor(error: OnboardingApiError): string {
  switch (error.status) {
    case 400:
      return 'Validation failed';
    case 404:
      return 'User not found';
    case 409:
      return 'Email already registered';
    case 500:
      return 'Server error';
    default:
      return 'Request failed';
  }
}
