package com.rentacar.service;

import com.rentacar.dto.CarUpdateRequest;
import com.rentacar.entity.Car;
import com.rentacar.exceptions.ApiException;
import com.rentacar.repository.CarRepository;
import com.rentacar.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarServiceImplTest {

    @Mock
    private CarRepository carRepository;
    @Mock
    private RentalRepository rentalRepository;
    @InjectMocks
    private CarServiceImpl carService;

    @Test
    void findById_throwsNotFoundWhenMissing() {
        when(carRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> carService.findById(1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void update_changesOnlyProvidedFields() {
        Car car = new Car();
        car.setId(1L);
        car.setPricePerDay(new BigDecimal("100.00"));
        car.setAvailable(true);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(inv -> inv.getArgument(0));

        Car updated = carService.update(1L, new CarUpdateRequest(new BigDecimal("150.00"), null, null));

        assertEquals(new BigDecimal("150.00"), updated.getPricePerDay());
        assertEquals(true, updated.isAvailable());
    }

    @Test
    void update_canMarkCarUnavailable() {
        Car car = new Car();
        car.setId(1L);
        car.setAvailable(true);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(inv -> inv.getArgument(0));

        Car updated = carService.update(1L, new CarUpdateRequest(null, false, null));

        assertFalse(updated.isAvailable());
    }
}
