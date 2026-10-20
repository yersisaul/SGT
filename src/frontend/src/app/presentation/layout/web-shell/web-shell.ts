import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Header } from '../header/header';
import { WebSidebar } from '../web-sidebar/web-sidebar';

/** Layout del panel del sitio web: mismo armazón que AppShell, con su propio menú. */
@Component({
  selector: 'app-web-shell',
  imports: [RouterOutlet, WebSidebar, Header],
  templateUrl: './web-shell.html',
  styleUrl: '../app-shell/app-shell.css',
})
export class WebShell {
  protected readonly sidebarOpen = signal(false);

  protected toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  protected closeSidebar(): void {
    this.sidebarOpen.set(false);
  }
}
