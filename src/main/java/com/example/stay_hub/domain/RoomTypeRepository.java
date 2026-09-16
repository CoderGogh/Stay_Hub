package com.example.stay_hub.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    Optional<RoomType> findByAccommodationAndSupplierRoomTypeCode(Accommodation accommodation, String supplierRoomTypeCode);

    List<RoomType> findByAccommodationIn(Collection<Accommodation> accommodations);
}
