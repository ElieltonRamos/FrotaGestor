import { Component, inject } from '@angular/core';
import { Driver } from '../../../interfaces/driver';
import { DriverService } from '../../../services/driver-service';
import { PaginatedResponse } from '../../../interfaces/paginator';
import { PaginatorComponent } from "../../../components/paginator/paginator.component";

@Component({
  selector: 'app-list-driver',
  imports: [PaginatorComponent],
  templateUrl: './list-driver.html',
  styles: ``,
})
export class ListDriver {
  private serviceDriver = inject(DriverService);

  drivers: Driver[] = [];
  total = 10;
  page = 1;
  limit = 5;
  totalPages = 10;

  ngOnInit(): void {
    // this.listDrivers();
    this.drivers = []
  }

  listDrivers(page: number = this.page, limit: number = this.limit) {
    this.serviceDriver.getAll(page, limit).subscribe((res: PaginatedResponse<Driver>) => {
      this.drivers = res.data;
      this.total = res.total;
      this.page = res.page;
      this.limit = res.limit;
      this.totalPages = res.totalPages;
    });
  }

  onPageChange(newPage: number) {
    this.listDrivers(newPage);
  }
}
