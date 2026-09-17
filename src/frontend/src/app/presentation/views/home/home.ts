import { Component } from '@angular/core';


@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {

currentIndex = 0;

  slides = [
    '/images/img3.jpg',
    '/images/img2.jpg',
    '/images/img1.jpg',
    '/images/img4.jpg'
  ];

  btnNext() {
    this.currentIndex =
      (this.currentIndex + 1) % this.slides.length;
  }

  btnPrev() {
    this.currentIndex =
      (this.currentIndex - 1 + this.slides.length) % this.slides.length;
  }

  goToSlide(index: number) {
    this.currentIndex = index;
  }
}


