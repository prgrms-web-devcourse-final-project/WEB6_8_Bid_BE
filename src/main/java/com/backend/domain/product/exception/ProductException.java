package com.backend.domain.product.exception;

import com.backend.global.exception.ServiceException;
import com.backend.global.response.RsStatus;

public class ProductException extends ServiceException {
    public ProductException(String resultCode, String msg) {
        super(resultCode, msg);
    }

    // ======================================= not found ======================================= //
    private static ProductException notFound(int detailCode, String msg) {
        return new ProductException(RsStatus.NOT_FOUND.getResultCode() + "-" + detailCode, msg);
    }

    // 404-1
    public static ProductException notFound() {
        return notFound(1, "존재하지 않는 상품입니다");
    }

    // 404-2
    public static ProductException imageNotFound() {
        return notFound(2, "존재하지 않는 이미지입니다");
    }

    // ======================================= forbidden ======================================= //
    private static ProductException forbidden(int detailCode, String msg) {
        return new ProductException(RsStatus.FORBIDDEN.getResultCode() + "-" + detailCode, msg);
    }

    // 403-1
    public static ProductException accessModifyForbidden() {
        return forbidden(1, "상품 수정 권한이 없습니다");
    }

    // 403-2
    public static ProductException accessDeleteForbidden() {
        return forbidden(2, "상품 삭제 권한이 없습니다");
    }

    // 403-3
    public static ProductException auctionModifyForbidden() {
        return forbidden(3, "경매 시작 시간이 지났으므로 상품 수정이 불가능합니다");
    }

    // 403-4
    public static ProductException auctionDeleteForbidden() {
        return forbidden(4, "경매 시작 시간이 지났으므로 상품 삭제가 불가능합니다");
    }

    // ======================================= bad request ======================================= //
    private static ProductException badRequest(int detail, String msg) {
        return new ProductException(RsStatus.BAD_REQUEST.getResultCode() + "-" + detail, msg);
    }

    // 400-1
    public static ProductException locationRequired() {
        return badRequest(1, "직거래 시 배송지는 필수입니다");
    }

    // 400-2
    public static ProductException imageRequired() {
        return badRequest(2, "이미지는 필수입니다");
    }

    // 400-3
    public static ProductException imageMaxCountExceeded() {
        return badRequest(3, "이미지는 최대 5개까지만 업로드할 수 있습니다");
    }

    // 400-4
    public static ProductException emptyFile() {
        return badRequest(4, "빈 파일은 업로드할 수 없습니다");
    }

    // 400-5
    public static ProductException fileTooLarge() {
        return badRequest(5, "이미지 파일 크기는 5MB를 초과할 수 없습니다");
    }

    // 400-6
    public static ProductException invalidFileName() {
        return badRequest(6, "올바른 파일명이 아닙니다");
    }

    // 400-7
    public static ProductException unsupportedFileType() {
        return badRequest(7, "지원하지 않는 파일 형식입니다 (jpg, jpeg, png, gif, webp만 가능)");
    }

    // 400-8
    public static ProductException fileUploadFailed() {
        return badRequest(8, "이미지 파일 업로드에 실패했습니다");
    }

    // 400-9
    public static ProductException imageNotBelongToProduct() {
        return badRequest(9, "이미지가 해당 상품에 속하지 않습니다");
    }
}
