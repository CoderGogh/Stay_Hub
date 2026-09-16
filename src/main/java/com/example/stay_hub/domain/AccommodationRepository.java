package com.example.stay_hub.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

    Optional<Accommodation> findBySupplierCodeAndSupplierHotelCode(SupplierCode supplierCode, String supplierHotelCode);
}
