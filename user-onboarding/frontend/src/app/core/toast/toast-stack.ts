import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-toast-stack',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './toast-stack.html',
  styleUrl: './toast-stack.scss',
})
export class ToastStack {
  protected readonly toastService = inject(ToastService);
}
