package com.example.stay_hub.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 객실 타입 코드는 "해당 숙소 안에서만" 유일하다는 부록 A.0 규약이
 * 실제 DB 제약(accommodation_id + supplier_room_type_code)으로 지켜지는지 검증한다.
 */
@DataJpaTest
class RoomTypeRepositoryTest {

    @Autowired
    private AccommodationRepository accommodationRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Test
    void 같은_숙소_안에서_같은_객실타입코드는_중복_저장할_수_없다() {
        Accommodation accommodation = accommodationRepository.saveAndFlush(
                new Accommodation(SupplierCode.SUPPLIER_A, "A-2001", "Hanul Bay Hotel"));
        roomTypeRepository.saveAndFlush(new RoomType(accommodation, "DLX-TWN", "Deluxe Twin", 2));

        assertThatThrownBy(() -> roomTypeRepository.saveAndFlush(
                new RoomType(accommodation, "DLX-TWN", "Duplicate", 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 다른_숙소라면_같은_객실타입코드도_각각_저장된다() {
        Accommodation hotelA = accommodationRepository.saveAndFlush(
                new Accommodation(SupplierCode.SUPPLIER_A, "A-2001", "Hotel A"));
        Accommodation hotelB = accommodationRepository.saveAndFlush(
                new Accommodation(SupplierCode.SUPPLIER_A, "A-2002", "Hotel B"));

        roomTypeRepository.saveAndFlush(new RoomType(hotelA, "DLX-TWN", "Deluxe Twin", 2));
        RoomType savedInHotelB = roomTypeRepository.saveAndFlush(
                new RoomType(hotelB, "DLX-TWN", "Deluxe Twin", 2));

        assertThat(savedInHotelB.getId()).isNotNull();
        assertThat(roomTypeRepository.findAll()).hasSize(2);
    }
}
