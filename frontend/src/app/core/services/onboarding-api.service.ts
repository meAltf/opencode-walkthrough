import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';

import {
  ApiResponse,
  FieldError,
  OnboardedUser,
  OnboardingApiError,
  OnboardingRequest,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class OnboardingApiService {
  private static readonly BASE = '/api/v1/users/onboarding';

  private readonly http = inject(HttpClient);

  onboard(request: OnboardingRequest): Observable<OnboardedUser> {
    return this.request<OnboardedUser>(this.http.post<ApiResponse<OnboardedUser>>(
      OnboardingApiService.BASE,
      request,
    ));
  }

  findById(id: string): Observable<OnboardedUser> {
    return this.request<OnboardedUser>(
      this.http.get<ApiResponse<OnboardedUser>>(`${OnboardingApiService.BASE}/${encodeURIComponent(id)}`),
    );
  }

  findByEmail(email: string): Observable<OnboardedUser> {
    const params = new HttpParams().set('email', email);
    return this.request<OnboardedUser>(
      this.http.get<ApiResponse<OnboardedUser>>(OnboardingApiService.BASE, { params }),
    );
  }

  private request<T>(source: Observable<ApiResponse<T>>): Observable<T> {
    return source.pipe(
      map((response) => {
        if (response.data === null) {
          throw new OnboardingApiError(response.code, response.message, [], 0);
        }
        return response.data;
      }),
      catchError((error: unknown) => throwError(() => toApiError(error))),
    );
  }
}

function toApiError(error: unknown): OnboardingApiError {
  if (error instanceof OnboardingApiError) {
    return error;
  }
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ApiResponse<FieldError[]> | undefined;
    if (body && typeof body.code === 'string') {
      return new OnboardingApiError(
        body.code,
        body.message,
        Array.isArray(body.data) ? body.data : [],
        error.status,
      );
    }
    if (error.status === 0) {
      return new OnboardingApiError(
        'CLIENT_NETWORK_ERROR',
        'Cannot reach the API. Is the Spring Boot app running on port 8080?',
        [],
        0,
      );
    }
    return new OnboardingApiError(
      'CLIENT_UNEXPECTED_ERROR',
      error.statusText || 'Unexpected error',
      [],
      error.status,
    );
  }
  return new OnboardingApiError('CLIENT_UNKNOWN_ERROR', String(error), [], 0);
}
