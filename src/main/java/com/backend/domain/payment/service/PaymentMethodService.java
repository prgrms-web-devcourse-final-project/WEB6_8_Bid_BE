package com.backend.domain.payment.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.payment.constant.PaymentMethodType;
import com.backend.domain.payment.dto.PaymentMethodCreateRequest;
import com.backend.domain.payment.dto.PaymentMethodResponse;
import com.backend.domain.payment.entity.PaymentMethod;
import com.backend.domain.payment.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;  // 결제수단 저장/조회
    private final MemberRepository memberRepository;                // 회원 로딩

    // 날짜 포맷(응답용)..
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 결제수단 등록
     * type=CARD → 카드 필드만 채우고 은행 필드는 null
     * type=BANK → 은행 필드만 채우고 카드 필드는 null
     */
    @Transactional
    public PaymentMethodResponse create(Long memberId, PaymentMethodCreateRequest req) {

        //  회원 조회(없으면 예외)..
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        // CARD/BANK 외 값이면 예외..
        PaymentMethodType type;
        try {
            type = PaymentMethodType.valueOf(String.valueOf(req.getType()).trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("type 은 CARD 또는 BANK 이어야 합니다.");
        }

        // 별칭 중복 검사(별칭이 있을 때만 체크)..
        if (req.getAlias() != null &&
                paymentMethodRepository.existsByMemberAndAlias(member, req.getAlias())) {
            throw new IllegalArgumentException("이미 사용 중인 별명(alias)입니다.");
        }

        // 기본 결제수단 지정 규칙..
        // - 사용자가 isDefault=true 로 요청하거나..
        // - 해당 회원의 첫 결제수단인 경우 자동 기본 지정..
        boolean shouldBeDefault =
                Boolean.TRUE.equals(req.getIsDefault()) ||
                        paymentMethodRepository.countByMember(member) == 0;

        // 새로 기본으로 설정할 경우, 기존 기본을 해제..
        if (shouldBeDefault) {
            paymentMethodRepository.findFirstByMemberAndIsDefaultTrue(member)
                    .ifPresent(pm -> pm.setIsDefault(false));
        }

        // 타입별 유효성 검증 & 반대 타입 필드 null 강제..
        String brand = null, last4 = null, bankCode = null, bankName = null, acctLast4 = null;
        Integer expMonth = null, expYear = null;

        switch (type) {
            case CARD -> {
                // 필수값 체크..
                if (isBlank(req.getBrand()) || isBlank(req.getLast4()) ||
                        req.getExpMonth() == null || req.getExpYear() == null) {
                    throw new IllegalArgumentException("CARD는 brand, last4, expMonth, expYear가 필요합니다.");
                }
                // 카드 필드 채우기..
                brand = req.getBrand();
                last4 = req.getLast4();
                expMonth = req.getExpMonth();
                expYear = req.getExpYear();

                // 은행 쪽은 명시적으로 null (가독성/안전성)..
                bankCode = null;
                bankName = null;
                acctLast4 = null;
            }
            case BANK -> {
                if (isBlank(req.getBankName()) || isBlank(req.getAcctLast4())) {
                    throw new IllegalArgumentException("BANK는 bankName, acctLast4가 필요합니다. (bankCode는 선택)");
                }
                // 은행 필드 채우기..
                bankCode = req.getBankCode();
                bankName = req.getBankName();
                acctLast4 = req.getAcctLast4();

                // 카드 쪽은 null..
                brand = null;
                last4 = null;
                expMonth = null;
                expYear = null;
            }
        }

        PaymentMethod entity = PaymentMethod.builder()
                .member(member)
                .type(type)
                .token(req.getToken())
                .alias(req.getAlias())
                .isDefault(shouldBeDefault)
                // 타입별 필드 적용..
                .brand(brand)
                .last4(last4)
                .expMonth(expMonth)
                .expYear(expYear)
                .bankCode(bankCode)
                .bankName(bankName)
                .acctLast4(acctLast4)
                .build();

        paymentMethodRepository.save(entity);
        return toResponse(entity);
    }

    // 결제 수단 다건 조회..
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> findAll(Long memberId) {
        // 회원 검증(없으면 404)..
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원이 존재하지 않습니다."));

        // 기본 수단 우선 → 최신 생성 순으로 정렬해 반환
        return paymentMethodRepository.findAllByMemberOrderByIsDefaultDescCreateDateDesc(member)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    //엔티티 → 응답 DTO 매핑..
    private PaymentMethodResponse toResponse(PaymentMethod e) {
        return PaymentMethodResponse.builder()
                .id(e.getId())
                .type(e.getType().name())
                .alias(e.getAlias())
                .isDefault(e.getIsDefault())

                .brand(e.getBrand())
                .last4(e.getLast4())
                .expMonth(e.getExpMonth())
                .expYear(e.getExpYear())

                .bankCode(e.getBankCode())
                .bankName(e.getBankName())
                .acctLast4(e.getAcctLast4())

                .createDate(e.getCreateDate() == null ? null : e.getCreateDate().format(DATE_TIME))
                .modifyDate(e.getModifyDate() == null ? null : e.getModifyDate().format(DATE_TIME))
                .expireDate((e.getExpYear() != null && e.getExpMonth() != null)
                        ? String.format("%04d-%02d", e.getExpYear(), e.getExpMonth())
                        : null)
                .build();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }


}
