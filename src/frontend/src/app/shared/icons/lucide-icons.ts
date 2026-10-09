import { ChangeDetectionStrategy, Component } from '@angular/core';

import * as data from './lucide-icon-data';
import { LUCIDE_ICON_TEMPLATE, LucideIconBase } from './lucide-icon';

/*
 * Iconos de Lucide usados por SGT, con la misma API que @lucide/angular
 * (clase LucideX, selector svg[lucideX], input [size]).
 *
 * Por qué no se usa el paquete directamente: es un único archivo de 12 MB con
 * ~1.850 componentes precompilados que el linker de Angular procesa completo
 * en cada build limpio (> 3,5 GB de RAM: inviable en Docker/CI). Para agregar
 * un icono: copiar su `node` desde https://lucide.dev a lucide-icon-data.ts y
 * declarar aquí su componente.
 */

@Component({
  selector: 'svg[lucideActivity]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideActivity extends LucideIconBase {
  protected readonly icon = data.activityIcon;
}

@Component({
  selector: 'svg[lucideArrowUpDown]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideArrowUpDown extends LucideIconBase {
  protected readonly icon = data.arrowUpDownIcon;
}

@Component({
  selector: 'svg[lucideBell]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideBell extends LucideIconBase {
  protected readonly icon = data.bellIcon;
}

@Component({
  selector: 'svg[lucideBoxes]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideBoxes extends LucideIconBase {
  protected readonly icon = data.boxesIcon;
}

@Component({
  selector: 'svg[lucideChevronLeft]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideChevronLeft extends LucideIconBase {
  protected readonly icon = data.chevronLeftIcon;
}

@Component({
  selector: 'svg[lucideChevronRight]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideChevronRight extends LucideIconBase {
  protected readonly icon = data.chevronRightIcon;
}

@Component({
  selector: 'svg[lucideCircleAlert]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideCircleAlert extends LucideIconBase {
  protected readonly icon = data.circleAlertIcon;
}

@Component({
  selector: 'svg[lucideCircleCheck]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideCircleCheck extends LucideIconBase {
  protected readonly icon = data.circleCheckIcon;
}

@Component({
  selector: 'svg[lucideCircleX]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideCircleX extends LucideIconBase {
  protected readonly icon = data.circleXIcon;
}

@Component({
  selector: 'svg[lucideClipboardCheck]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideClipboardCheck extends LucideIconBase {
  protected readonly icon = data.clipboardCheckIcon;
}

@Component({
  selector: 'svg[lucideClipboardList]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideClipboardList extends LucideIconBase {
  protected readonly icon = data.clipboardListIcon;
}

@Component({
  selector: 'svg[lucideClock]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideClock extends LucideIconBase {
  protected readonly icon = data.clockIcon;
}

@Component({
  selector: 'svg[lucideDownload]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideDownload extends LucideIconBase {
  protected readonly icon = data.downloadIcon;
}

@Component({
  selector: 'svg[lucideInbox]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideInbox extends LucideIconBase {
  protected readonly icon = data.inboxIcon;
}

@Component({
  selector: 'svg[lucideKeyRound]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideKeyRound extends LucideIconBase {
  protected readonly icon = data.keyRoundIcon;
}

@Component({
  selector: 'svg[lucideLayers]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideLayers extends LucideIconBase {
  protected readonly icon = data.layersIcon;
}

@Component({
  selector: 'svg[lucideLayoutDashboard]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideLayoutDashboard extends LucideIconBase {
  protected readonly icon = data.layoutDashboardIcon;
}

@Component({
  selector: 'svg[lucideLayoutGrid]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideLayoutGrid extends LucideIconBase {
  protected readonly icon = data.layoutGridIcon;
}

@Component({
  selector: 'svg[lucideListChecks]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideListChecks extends LucideIconBase {
  protected readonly icon = data.listChecksIcon;
}

@Component({
  selector: 'svg[lucideLock]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideLock extends LucideIconBase {
  protected readonly icon = data.lockIcon;
}

@Component({
  selector: 'svg[lucideLogOut]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideLogOut extends LucideIconBase {
  protected readonly icon = data.logOutIcon;
}

@Component({
  selector: 'svg[lucideMenu]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideMenu extends LucideIconBase {
  protected readonly icon = data.menuIcon;
}

@Component({
  selector: 'svg[lucideMoon]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideMoon extends LucideIconBase {
  protected readonly icon = data.moonIcon;
}

@Component({
  selector: 'svg[lucidePaperclip]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucidePaperclip extends LucideIconBase {
  protected readonly icon = data.paperclipIcon;
}

@Component({
  selector: 'svg[lucidePencil]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucidePencil extends LucideIconBase {
  protected readonly icon = data.pencilIcon;
}

@Component({
  selector: 'svg[lucidePlus]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucidePlus extends LucideIconBase {
  protected readonly icon = data.plusIcon;
}

@Component({
  selector: 'svg[lucideSearch]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideSearch extends LucideIconBase {
  protected readonly icon = data.searchIcon;
}

@Component({
  selector: 'svg[lucideShieldCheck]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideShieldCheck extends LucideIconBase {
  protected readonly icon = data.shieldCheckIcon;
}

@Component({
  selector: 'svg[lucideSun]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideSun extends LucideIconBase {
  protected readonly icon = data.sunIcon;
}

@Component({
  selector: 'svg[lucideTable]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideTable extends LucideIconBase {
  protected readonly icon = data.tableIcon;
}

@Component({
  selector: 'svg[lucideTrash]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideTrash extends LucideIconBase {
  protected readonly icon = data.trashIcon;
}

@Component({
  selector: 'svg[lucideTrendingUp]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideTrendingUp extends LucideIconBase {
  protected readonly icon = data.trendingUpIcon;
}

@Component({
  selector: 'svg[lucideUpload]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideUpload extends LucideIconBase {
  protected readonly icon = data.uploadIcon;
}

@Component({
  selector: 'svg[lucideUsers]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideUsers extends LucideIconBase {
  protected readonly icon = data.usersIcon;
}

@Component({
  selector: 'svg[lucideWrench]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideWrench extends LucideIconBase {
  protected readonly icon = data.wrenchIcon;
}

@Component({
  selector: 'svg[lucideX]',
  template: LUCIDE_ICON_TEMPLATE,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LucideX extends LucideIconBase {
  protected readonly icon = data.xIcon;
}
