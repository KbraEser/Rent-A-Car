package com.rentacar.service;

import com.rentacar.dto.RentalRequest;
import com.rentacar.dto.RentalResponse;
import com.rentacar.entity.Car;
import com.rentacar.entity.Rental;
import com.rentacar.entity.User;
import com.rentacar.enums.RentalStatus;
import com.rentacar.exceptions.ApiException;
import com.rentacar.repository.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServiceImplTest {

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private CarService carService;
    @InjectMocks
    private RentalServiceImpl rentalService;

    private User user;
    private Car car;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        car = new Car();
        car.setId(10L);
        car.setMake("Toyota");
        car.setModel("Corolla");
        car.setYear(2022);
        car.setCity("Istanbul");
        car.setPricePerDay(new BigDecimal("100.00"));
        car.setAvailable(true);
    }

    private RentalRequest request(LocalDate start, LocalDate end) {
        RentalRequest request = new RentalRequest();
        request.setCarId(10L);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setLocation("Kadıköy");
        return request;
    }

    @Test
    void create_calculatesTotalPriceFromDays() {
        LocalDate start = LocalDate.now().plusDays(1);
        when(carService.findById(10L)).thenReturn(car);
        when(rentalRepository.save(any(Rental.class))).thenAnswer(inv -> inv.getArgument(0));

        RentalResponse response = rentalService.create(request(start, start.plusDays(3)), user);

        assertEquals(new BigDecimal("300.00"), response.getTotalPrice());
        assertEquals(RentalStatus.active, response.getStatus());
    }

    @Test
    void create_throwsWhenEndDateBeforeStartDate() {
        LocalDate start = LocalDate.now().plusDays(5);

        ApiException ex = assertThrows(ApiException.class,
                () -> rentalService.create(request(start, start.minusDays(1)), user));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenCarNotAvailable() {
        LocalDate start = LocalDate.now().plusDays(1);
        car.setAvailable(false);
        when(carService.findById(10L)).thenReturn(car);

        ApiException ex = assertThrows(ApiException.class,
                () -> rentalService.create(request(start, start.plusDays(2)), user));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void create_throwsConflictWhenDatesOverlap() {
        LocalDate start = LocalDate.now().plusDays(1);
        when(carService.findById(10L)).thenReturn(car);
        when(rentalRepository.existsByCarIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                any(), any(), any(), any())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> rentalService.create(request(start, start.plusDays(2)), user));

        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    @Test
    void cancel_setsStatusToCancelled() {
        Rental rental = rentalOf(user, RentalStatus.active);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(inv -> inv.getArgument(0));

        RentalResponse response = rentalService.cancel(5L, user);

        assertEquals(RentalStatus.cancelled, response.getStatus());
    }

    @Test
    void cancel_throwsForbiddenForOtherUsersRental() {
        User owner = new User();
        owner.setId(99L);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(rentalOf(owner, RentalStatus.active)));

        ApiException ex = assertThrows(ApiException.class, () -> rentalService.cancel(5L, user));

        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    void cancel_throwsNotFoundWhenRentalMissing() {
        when(rentalRepository.findById(5L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> rentalService.cancel(5L, user));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    private Rental rentalOf(User owner, RentalStatus status) {
        Rental rental = new Rental();
        rental.setId(5L);
        rental.setUser(owner);
        rental.setCar(car);
        rental.setStatus(status);
        rental.setStartDate(LocalDate.now().plusDays(1));
        rental.setEndDate(LocalDate.now().plusDays(3));
        rental.setTotalPrice(new BigDecimal("200.00"));
        rental.setCity("Istanbul");
        rental.setLocation("Kadıköy");
        return rental;
    }
}
