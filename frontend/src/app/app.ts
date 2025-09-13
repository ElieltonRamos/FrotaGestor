import { Component, inject, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Header } from './components/header/header';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header],
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
  private router = inject(Router);

  isLoginRoute(): boolean {
    return this.router.url === '/';
  }
}
