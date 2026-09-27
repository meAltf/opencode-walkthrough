import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ToastStack } from './core/toast/toast-stack';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastStack],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
