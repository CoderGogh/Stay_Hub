package com.example.stay_hub.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 DB(H2) 유니크 제약이 설계 원칙을 강제하는지 검증한다.
 * "같은 공급사 상품은 항상 같은 내부 식별자로 조회되어야 한다"는 필수 조건이므로
 * 서비스 로직뿐 아니라 DB 제약 수준에서도 보장되는지 직접 확인한다.
 */
@DataJpaTest
class AccommodationRepositoryTest {

    @Autowired
    private AccommodationRepository accommodationRepository;

    @Test
    void 같은_공급사의_같은_숙소코드는_중복_저장할_수_없다() {
        accommodationRepository.saveAndFlush(
                new Accommodation(SupplierCode.SUPPLIER_A, "A-2001", "Hanul Bay Hotel"));

        assertThatThrownBy(() -> accommodationRepository.saveAndFlush(
                new Accommodation(SupplierCode.SUPPLIER_A, "A-2001", "Duplicate Entry")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 공급사가_다르면_같은_숙소코드도_각각_저장된다() {
        accommodationRepository.saveAndFlush(
                new Accommodation(SupplierCode.SUPPLIER_A, "SAME-CODE", "A쪽 표기"));

        Accommodation savedB = accommodationRepository.saveAndFlush(
                new Accommodation(SupplierCode.SUPPLIER_B, "SAME-CODE", "B쪽 표기"));

        assertThat(savedB.getId()).isNotNull();
        assertThat(accommodationRepository.findAll()).hasSize(2);
    }

    @Test
    void 같은_공급사_상품을_다시_조회해도_동일한_내부_식별자를_돌려준다() {
        Accommodation saved = accommodationRepository.saveAndFlush(
                new Accommodation(SupplierCode.SUPPLIER_A, "A-2001", "Hanul Bay Hotel"));

        Accommodation found = accommodationRepository
                .findBySupplierCodeAndSupplierHotelCode(SupplierCode.SUPPLIER_A, "A-2001")
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
    }
}
