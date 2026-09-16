package com.example.stay_hub.adapter;

import com.example.stay_hub.domain.SupplierCode;

/**
 * 공급사 호출 실패 통일 예외.
 * HTTP 상태 코드 실패 / 'HTTP 200 + 본문 resultCode' 실패 -> 이 예외 하나로 통일 (부록 A.2 대응).
 */
public class SupplierCallException extends RuntimeException {

    private final SupplierCode supplierCode;

    public SupplierCallException(SupplierCode supplierCode, String message) {
        super(message);
        this.supplierCode = supplierCode;
    }

    public SupplierCallException(SupplierCode supplierCode, String message, Throwable cause) {
        super(message, cause);
        this.supplierCode = supplierCode;
    }

    public SupplierCode getSupplierCode() {
        return supplierCode;
    }
}
