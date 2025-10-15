package com.backend.domain.payment.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.payment.dto.response.TossIssueBillingKeyResponse;
import com.backend.domain.payment.enums.PaymentMethodType;
import com.backend.domain.payment.dto.request.PaymentMethodCreateRequest;
import com.backend.domain.payment.dto.response.PaymentMethodDeleteResponse;
import com.backend.domain.payment.dto.request.PaymentMethodEditRequest;
import com.backend.domain.payment.dto.response.PaymentMethodResponse;
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
                paymentMethodRepository.existsByMemberAndAliasAndDeletedFalse(member, req.getAlias())) {
            throw new IllegalArgumentException("이미 사용 중인 별명(alias)입니다.");
        }

        // 기본 결제수단 지정 규칙..
        // - 사용자가 isDefault=true 로 요청하거나..
        // - 해당 회원의 첫 결제수단인 경우 자동 기본 지정..
        boolean shouldBeDefault =
                Boolean.TRUE.equals(req.getIsDefault()) ||
                        paymentMethodRepository.countByMemberAndDeletedFalse(member) == 0;

        // 새로 기본으로 설정할 경우, 기존 기본을 해제..
        if (shouldBeDefault) {
            paymentMethodRepository.findFirstByMemberAndIsDefaultTrueAndDeletedFalse(member)
                    .ifPresent(pm -> pm.setIsDefault(false));
        }

        // 타입별 유효성 검증 & 반대 타입 필드 null 강제..
        String brand = null, last4 = null, bankCode = null, bankName = null, acctLast4 = null;
        Integer expMonth = null, expYear = null;

        switch (type) {
            case CARD -> {
                // 필수값 체크..
                if (isBlank(req.getBrand()) || isBlank(req.getLast4())){
                    throw new IllegalArgumentException("CARD는 brand, last4가 필요합니다. (expMonth/expYear는 선택)");
                }

                String normalizedLast4 = req.getLast4().replaceAll("\\D", "");
                if (normalizedLast4.length() < 4) {
                    throw new IllegalArgumentException("last4는 카드번호 끝 4자리여야 합니다.");
                }
                last4 = normalizedLast4.substring(normalizedLast4.length() - 4);

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

        String provider = req.getProvider();
        if (provider == null || provider.isBlank()) {
            provider = "toss";
        } else {
            provider = provider.trim().toLowerCase();
        }

        PaymentMethod entity = PaymentMethod.builder()
                .member(member)
                .methodType(type)
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
                .provider(provider)
                .active(true)
                .deleted(false)
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
        return paymentMethodRepository.findAllByMemberAndDeletedFalseOrderByIsDefaultDescCreateDateDesc(member)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 결제 수단 단건 조회..
    @Transactional(readOnly = true)
    public PaymentMethodResponse findOne(Long memberId, Long paymentMethodId) {
        // 회원 검증..
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원이 존재하지 않습니다."));

        // 본인 소유 결제수단만 조회..
        PaymentMethod entity = paymentMethodRepository.findByIdAndMemberAndDeletedFalse(paymentMethodId, member)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 수단을 찾을 수 없습니다."));

        return toResponse(entity);
    }

    // 결제 수단 수정 - 타입 변경 불가...
    @Transactional
    public PaymentMethodResponse edit(Long memberId, Long paymentMethodId, PaymentMethodEditRequest req) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원이 존재하지 않습니다."));
        PaymentMethod entity = paymentMethodRepository.findByIdAndMemberAndDeletedFalse(paymentMethodId, member)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 수단을 찾을 수 없습니다."));

        req.setAlias(nvlBlankToNull(req.getAlias()));

        // 별칭..
        if (req.getAlias() != null) {
            String alias = req.getAlias();
            if (!alias.isEmpty()
                    && paymentMethodRepository.existsByMemberAndAliasAndIdNotAndDeletedFalse(member, alias, entity.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 별명(alias)입니다.");
            }
            entity.setAlias(alias.isEmpty() ? null : alias);
        }

        // 기본 여부..
        if (req.getIsDefault() != null) {
            if (Boolean.TRUE.equals(req.getIsDefault())) {
                paymentMethodRepository.findFirstByMemberAndIsDefaultTrueAndDeletedFalse(member)
                        .ifPresent(pm -> { if (!pm.getId().equals(entity.getId())) pm.setIsDefault(false); });
                entity.setIsDefault(true);
            } else {
                entity.setIsDefault(false);
            }
        }

        return toResponse(entity);
    }

    // 공백/빈문자 → null
    private String nvlBlankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }


    // 회원의 결제수단을 삭제하고 그게 기본 수단이었으면 다른 수단 중 가장 최근 것을 자동으로 기본으로 승계..
    @Transactional
    public PaymentMethodDeleteResponse deleteAndReport(Long memberId, Long paymentMethodId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원이 존재하지 않습니다."));

        // 본인 소유 + 삭제 안 된 수단 찾기..
        PaymentMethod target = paymentMethodRepository.findByIdAndMemberAndDeletedFalse(paymentMethodId, member)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 수단을 찾을 수 없습니다."));

        boolean wasDefault = Boolean.TRUE.equals(target.getIsDefault());

        paymentMethodRepository.delete(target);

        Long newDefaultId = null;
        if (wasDefault) {
            // 기본이 없어졌으니, 가장 최근 만든 수단을 새 기본으로..
            newDefaultId = paymentMethodRepository.findFirstByMemberAndDeletedFalseOrderByCreateDateDesc(member)
                    .map(pm -> { pm.setIsDefault(true); return pm.getId(); })
                    .orElse(null);
        }

        return PaymentMethodDeleteResponse.builder()
                .id(paymentMethodId)
                .deleted(true)
                .wasDefault(wasDefault)
                .newDefaultId(newDefaultId)
                .build();
    }

    @Transactional
    public PaymentMethodResponse saveOrUpdateBillingKey(Long memberId, TossIssueBillingKeyResponse res) {
        // 0) 파라미터 검증
        if (res == null || res.getBillingKey() == null || res.getBillingKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 billingKey 응답입니다.");
        }

        // 1) 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원이 존재하지 않습니다."));

        // 2) 기존 billingKey 보유 수단 조회(있으면 업데이트, 없으면 생성)
        PaymentMethod pm = paymentMethodRepository
                .findFirstByMemberAndTokenAndDeletedFalse(member, res.getBillingKey())
                .orElse(null);

        // 공통 스냅샷 값 정리
        String brand = res.getBrand();
        String last4 = res.getLast4();
        if (last4 != null) {
            String digits = last4.replaceAll("\\D", "");
            if (digits.length() >= 4) last4 = digits.substring(digits.length() - 4);
            else last4 = null;
        }
        Integer expMonth = res.getExpMonth();
        Integer expYear  = res.getExpYear();

        if (pm == null) {
            // 첫 등록이면 기본수단으로 지정
            boolean shouldBeDefault = paymentMethodRepository.countByMemberAndDeletedFalse(member) == 0;

            pm = PaymentMethod.builder()
                    .member(member)
                    .methodType(PaymentMethodType.CARD)
                    .provider("toss")
                    .token(res.getBillingKey())   // ⬅ billingKey 저장 (결제에 쓰는 token)
                    .alias(null)                  // 필요하면 FE에서 별칭 입력받아 수정 가능
                    .isDefault(shouldBeDefault)
                    .brand(brand)
                    .last4(last4)
                    .expMonth(expMonth)
                    .expYear(expYear)
                    .active(true)
                    .deleted(false)
                    .build();

            // 기존 기본 해제는 shouldBeDefault일 때만
            if (shouldBeDefault) {
                paymentMethodRepository.findFirstByMemberAndIsDefaultTrueAndDeletedFalse(member)
                        .ifPresent(old -> old.setIsDefault(false));
            }
        } else {
            // 기존 수단 스냅샷 갱신
            pm.setBrand(brand);
            pm.setLast4(last4);
            pm.setExpMonth(expMonth);
            pm.setExpYear(expYear);
            pm.setActive(true);
        }

        paymentMethodRepository.save(pm);
        return toResponse(pm);
    }

    //엔티티 → 응답 DTO 매핑..
    private PaymentMethodResponse toResponse(PaymentMethod e) {
        return PaymentMethodResponse.builder()
                .id(e.getId())

                .type(e.getMethodType().name())
                .alias(e.getAlias())
                .isDefault(e.getIsDefault())

                .provider(e.getProvider())
                .brand(e.getBrand())
                .last4(e.getLast4())
                .expMonth(e.getExpMonth())
                .expYear(e.getExpYear())

                .bankCode(e.getBankCode())
                .bankName(e.getBankName())
                .acctLast4(e.getAcctLast4())

                .createDate(e.getCreateDate())
                .modifyDate(e.getModifyDate())
                .expireDate((e.getExpYear() != null && e.getExpMonth() != null)
                        ? String.format("%04d-%02d", e.getExpYear(), e.getExpMonth())
                        : null)
                .build();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
