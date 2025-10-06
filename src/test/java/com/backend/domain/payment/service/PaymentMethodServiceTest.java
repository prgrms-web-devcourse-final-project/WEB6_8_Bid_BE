package com.backend.domain.payment.service;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.domain.payment.enums.PaymentMethodType;
import com.backend.domain.payment.dto.PaymentMethodCreateRequest;
import com.backend.domain.payment.dto.PaymentMethodDeleteResponse;
import com.backend.domain.payment.dto.PaymentMethodEditRequest;
import com.backend.domain.payment.dto.PaymentMethodResponse;
import com.backend.domain.payment.entity.PaymentMethod;
import com.backend.domain.payment.repository.PaymentMethodRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PaymentMethodService paymentMethodService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).email("test@example.com").build();
    }

    private PaymentMethod cardEntity(Long id, Member m) {
        PaymentMethod e = PaymentMethod.builder()
                .member(m)
                .type(PaymentMethodType.CARD)
                .alias("카드별칭")
                .isDefault(false)
                .brand("VISA")
                .last4("1111")
                .expMonth(12)
                .expYear(2030)
                .build();
        ReflectionTestUtils.setField(e, "id", id);   // ★ id 주입
        return e;
    }

    private PaymentMethod bankEntity(Long id, Member m) {
        PaymentMethod e = PaymentMethod.builder()
                .member(m)
                .type(PaymentMethodType.BANK)
                .alias("계좌별칭")
                .isDefault(false)
                .bankCode("004")
                .bankName("KB")
                .acctLast4("4321")
                .build();
        ReflectionTestUtils.setField(e, "id", id);   // ★ id 주입
        return e;
    }

    private PaymentMethodCreateRequest baseReq(String type) {
        PaymentMethodCreateRequest r = new PaymentMethodCreateRequest();
        r.setType(type);
        r.setToken("pg_tok");
        r.setAlias("별명1");
        r.setIsDefault(true);
        return r;
    }

    @Nested
    class CreateSuccess {

        @Test
        @DisplayName("CARD 타입 저장 시, 카드 필드만 채워지고 은행 필드는 null")
        void createCard_success() {
            // given
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(paymentMethodRepository.existsByMemberAndAliasAndDeletedFalse(member, "별명1")).thenReturn(false);
            // isDefault=true → shouldBeDefault=true → 기존 기본 해제 시도, 없다고 가정
            when(paymentMethodRepository.findFirstByMemberAndIsDefaultTrueAndDeletedFalse(member))
                    .thenReturn(Optional.empty());
            when(paymentMethodRepository.save(any(PaymentMethod.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PaymentMethodCreateRequest req = baseReq("CARD");
            req.setBrand("VISA");
            req.setLast4("1234");
            req.setExpMonth(12);
            req.setExpYear(2030);

            // when
            PaymentMethodResponse res = paymentMethodService.create(1L, req);

            // then
            assertThat(res.getType()).isEqualTo(PaymentMethodType.CARD.name());
            assertThat(res.getBrand()).isEqualTo("VISA");
            assertThat(res.getLast4()).isEqualTo("1234");
            assertThat(res.getExpMonth()).isEqualTo(12);
            assertThat(res.getExpYear()).isEqualTo(2030);
            assertThat(res.getBankCode()).isNull();
            assertThat(res.getBankName()).isNull();
            assertThat(res.getAcctLast4()).isNull();

            ArgumentCaptor<PaymentMethod> captor = ArgumentCaptor.forClass(PaymentMethod.class);
            verify(paymentMethodRepository).save(captor.capture());
            PaymentMethod saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(PaymentMethodType.CARD);
            assertThat(saved.getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("BANK 타입 저장 시, 은행 필드만 채워지고 카드 필드는 null")
        void createBank_success() {
            // given
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(paymentMethodRepository.existsByMemberAndAliasAndDeletedFalse(member, "별명1")).thenReturn(false);
            // isDefault=true → 기존 기본 하나 있다고 가정
            when(paymentMethodRepository.findFirstByMemberAndIsDefaultTrueAndDeletedFalse(member))
                    .thenReturn(Optional.of(PaymentMethod.builder().member(member).isDefault(true).build()));
            when(paymentMethodRepository.save(any(PaymentMethod.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PaymentMethodCreateRequest req = baseReq("BANK");
            req.setBankCode("004");
            req.setBankName("KB국민은행");
            req.setAcctLast4("5678");

            // when
            PaymentMethodResponse res = paymentMethodService.create(1L, req);

            // then
            assertThat(res.getType()).isEqualTo(PaymentMethodType.BANK.name());
            assertThat(res.getBankCode()).isEqualTo("004");
            assertThat(res.getBankName()).isEqualTo("KB국민은행");
            assertThat(res.getAcctLast4()).isEqualTo("5678");
            assertThat(res.getBrand()).isNull();
            assertThat(res.getLast4()).isNull();
            assertThat(res.getExpMonth()).isNull();
            assertThat(res.getExpYear()).isNull();
        }

        @Test
        @DisplayName("기본수단 우선→ 최신 생성순(리포지토리 반환 순서 가정)으로 매핑되어 응답한다")
        void findAll_success_order_and_mapping() {
            // given
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            PaymentMethod defaultCard = PaymentMethod.builder()
                    .member(member)
                    .type(PaymentMethodType.CARD)
                    .alias("내 주력카드")
                    .isDefault(true)
                    .brand("SHINHAN")
                    .last4("1111")
                    .expMonth(12)
                    .expYear(2030)
                    .build();

            PaymentMethod bank = PaymentMethod.builder()
                    .member(member)
                    .type(PaymentMethodType.BANK)
                    .alias("급여통장")
                    .isDefault(false)
                    .bankCode("004")
                    .bankName("KB국민은행")
                    .acctLast4("5678")
                    .build();

            // 정렬된 리스트(기본 수단 먼저)를 그대로 리턴
            when(paymentMethodRepository.findAllByMemberAndDeletedFalseOrderByIsDefaultDescCreateDateDesc(member))
                    .thenReturn(List.of(defaultCard, bank));

            // when
            List<PaymentMethodResponse> list = paymentMethodService.findAll(1L);

            // then
            assertThat(list).hasSize(2);

            // 1) 기본 수단(CARD) 먼저
            PaymentMethodResponse r1 = list.get(0);
            assertThat(r1.getType()).isEqualTo("CARD");
            assertThat(r1.getAlias()).isEqualTo("내 주력카드");
            assertThat(r1.getIsDefault()).isTrue();
            assertThat(r1.getBrand()).isEqualTo("SHINHAN");
            assertThat(r1.getLast4()).isEqualTo("1111");
            assertThat(r1.getExpMonth()).isEqualTo(12);
            assertThat(r1.getExpYear()).isEqualTo(2030);
            // expireDate 포맷 "YYYY-MM" 확인
            assertThat(r1.getExpireDate()).isEqualTo("2030-12");
            // 은행 필드는 null
            assertThat(r1.getBankCode()).isNull();
            assertThat(r1.getBankName()).isNull();
            assertThat(r1.getAcctLast4()).isNull();

            // 2) 그 다음 BANK
            PaymentMethodResponse r2 = list.get(1);
            assertThat(r2.getType()).isEqualTo("BANK");
            assertThat(r2.getAlias()).isEqualTo("급여통장");
            assertThat(r2.getIsDefault()).isFalse();
            assertThat(r2.getBankCode()).isEqualTo("004");
            assertThat(r2.getBankName()).isEqualTo("KB국민은행");
            assertThat(r2.getAcctLast4()).isEqualTo("5678");
            // 카드 필드는 null
            assertThat(r2.getBrand()).isNull();
            assertThat(r2.getLast4()).isNull();
            assertThat(r2.getExpMonth()).isNull();
            assertThat(r2.getExpYear()).isNull();

            verify(paymentMethodRepository).findAllByMemberAndDeletedFalseOrderByIsDefaultDescCreateDateDesc(member);
        }
    }

    @Nested
    class CreateFail {
        @Test
        @DisplayName("회원이 없으면 예외")
        void memberNotFound() {
            when(memberRepository.findById(1L)).thenReturn(Optional.empty());
            PaymentMethodCreateRequest req = baseReq("CARD");

            assertThatThrownBy(() -> paymentMethodService.create(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("회원이 존재하지 않습니다.");
        }

        @Test
        @DisplayName("type 이 CARD/BANK 가 아니면 예외")
        void invalidType() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            PaymentMethodCreateRequest req = baseReq("CASH");

            assertThatThrownBy(() -> paymentMethodService.create(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("type 은 CARD 또는 BANK 이어야 합니다.");
        }

        @Test
        @DisplayName("CARD 필수 누락 시 예외")
        void cardMissingFields() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            PaymentMethodCreateRequest req = baseReq("CARD");
            req.setBrand("VISA");
            req.setLast4(null); // 누락
            req.setExpMonth(12);
            req.setExpYear(2030);

            assertThatThrownBy(() -> paymentMethodService.create(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CARD는 brand, last4, expMonth, expYear가 필요합니다.");
        }

        @Test
        @DisplayName("BANK 필수 누락 시 예외")
        void bankMissingFields() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            PaymentMethodCreateRequest req = baseReq("BANK");
            req.setBankName(null); // 누락
            req.setAcctLast4("5678");

            assertThatThrownBy(() -> paymentMethodService.create(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BANK는 bankName, acctLast4가 필요합니다.");
        }

        @Test
        @DisplayName("별명(alias) 중복 시 예외")
        void aliasDuplicate() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(paymentMethodRepository.existsByMemberAndAliasAndDeletedFalse(member, "별명1")).thenReturn(true);

            PaymentMethodCreateRequest req = baseReq("CARD");
            req.setBrand("VISA");
            req.setLast4("1234");
            req.setExpMonth(12);
            req.setExpYear(2030);

            assertThatThrownBy(() -> paymentMethodService.create(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 사용 중인 별명");
        }
    }

    @Test
    @DisplayName("새 기본 결제수단 지정 시 기존 기본 해제")
    void newDefaultUnsetsOldDefault() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.existsByMemberAndAliasAndDeletedFalse(member, "별명1")).thenReturn(false);
        // 기존 기본 존재
        PaymentMethod oldDefault = PaymentMethod.builder()
                .member(member)
                .isDefault(true)
                .build();
        when(paymentMethodRepository.findFirstByMemberAndIsDefaultTrueAndDeletedFalse(member))
                .thenReturn(Optional.of(oldDefault));
        when(paymentMethodRepository.save(any(PaymentMethod.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentMethodCreateRequest req = baseReq("CARD");
        req.setBrand("VISA");
        req.setLast4("4444");
        req.setExpMonth(10);
        req.setExpYear(2031);
        req.setIsDefault(true);

        paymentMethodService.create(1L, req);

        assertThat(oldDefault.getIsDefault()).isFalse();
        ArgumentCaptor<PaymentMethod> captor = ArgumentCaptor.forClass(PaymentMethod.class);
        verify(paymentMethodRepository).save(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("내 결제수단 단건 조회")
    void success() {
        // given
        Long memberId = 1L;
        Long methodId = 12L;

        Member member = Member.builder()
                .id(memberId)
                .email("me@example.com")
                .build();

        PaymentMethod entity = PaymentMethod.builder()
                .member(member)
                .type(PaymentMethodType.CARD) // CARD | BANK
                .alias("결혼식 카드")
                .brand("SHINHAN")
                .last4("1234")
                .expMonth(12)
                .expYear(2027)
                .isDefault(true)
                .build();

        OffsetDateTime odt = OffsetDateTime.parse("2025-09-23T12:34:56Z");
        LocalDateTime ldt = odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        ReflectionTestUtils.setField(entity, "id", methodId);
        ReflectionTestUtils.setField(entity, "createDate", ldt);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(methodId, member)).thenReturn(Optional.of(entity));

        // when
        PaymentMethodResponse res = paymentMethodService.findOne(memberId, methodId);

        // then (필요한 핵심 필드만 검증)
        assertThat(res.getId()).isEqualTo(methodId);
        assertThat(res.getType()).isEqualTo("CARD");
        assertThat(res.getAlias()).isEqualTo("결혼식 카드");

        verify(memberRepository, times(1)).findById(memberId);
        verify(paymentMethodRepository, times(1)).findByIdAndMemberAndDeletedFalse(methodId, member);
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 404")
    void memberNotFound() {
        // given
        Long memberId = 1L;
        Long methodId = 12L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentMethodService.findOne(memberId, methodId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("회원이 존재하지 않습니다.");
                });

        verify(memberRepository, times(1)).findById(memberId);
        verify(paymentMethodRepository, never()).findByIdAndMemberAndDeletedFalse(anyLong(), any());
    }

    @Test
    @DisplayName("본인 소유 결제수단이 아니거나 없으면 404")
    void paymentMethodNotFoundForMember() {
        // given
        Long memberId = 1L;
        Long methodId = 99L;

        Member member = Member.builder()
                .id(memberId)
                .email("me@example.com")
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(methodId, member)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentMethodService.findOne(memberId, methodId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("결제 수단을 찾을 수 없습니다.");
                });

        verify(memberRepository, times(1)).findById(memberId);
        verify(paymentMethodRepository, times(1)).findByIdAndMemberAndDeletedFalse(methodId, member);
    }

    @Test
    @DisplayName("CARD: 카드 필드만 부분 수정, BANK 필드는 null로 보장")
    void edit_card_success_updateCardFields() {
        // given
        PaymentMethod entity = cardEntity(10L, member);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(10L, member)).thenReturn(Optional.of(entity));
        when(paymentMethodRepository.existsByMemberAndAliasAndIdNotAndDeletedFalse(any(), anyString(), anyLong()))
                .thenReturn(false);

        PaymentMethodEditRequest req = new PaymentMethodEditRequest();
        req.setAlias("경조/여행 전용");
        req.setIsDefault(true);
        req.setBrand("SHINHAN");
        req.setLast4("2222");
        req.setExpMonth(5);
        req.setExpYear(2035);
        // BANK 필드 미전달 (또는 null)

        // when
        PaymentMethodResponse res = paymentMethodService.edit(1L, 10L, req);

        // then
        assertThat(res.getAlias()).isEqualTo("경조/여행 전용");
        assertThat(res.getIsDefault()).isTrue();
        assertThat(res.getBrand()).isEqualTo("SHINHAN");
        assertThat(res.getLast4()).isEqualTo("2222");
        assertThat(res.getExpMonth()).isEqualTo(5);
        assertThat(res.getExpYear()).isEqualTo(2035);

        assertThat(res.getBankCode()).isNull();
        assertThat(res.getBankName()).isNull();
        assertThat(res.getAcctLast4()).isNull();
    }

    @Test
    @DisplayName("CARD: 교차 타입(BANK) 필드가 값으로 오면 400")
    void edit_card_reject_bankFields() {
        PaymentMethod entity = cardEntity(10L, member);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(10L, member)).thenReturn(Optional.of(entity));

        PaymentMethodEditRequest req = new PaymentMethodEditRequest();
        req.setBankName("KB"); // 교차 타입 값

        assertThatThrownBy(() -> paymentMethodService.edit(1L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("CARD: BANK 필드가 빈문자/공백이면 무시(정규화)되어 성공")
    void edit_card_blank_bankFields_areIgnored() {
        PaymentMethod entity = cardEntity(10L, member);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(10L, member)).thenReturn(Optional.of(entity));
        when(paymentMethodRepository.existsByMemberAndAliasAndIdNotAndDeletedFalse(any(), anyString(), anyLong()))
                .thenReturn(false);

        PaymentMethodEditRequest req = new PaymentMethodEditRequest();
        req.setAlias("새 별칭");
        req.setBankName("   "); // 공백 → null 정규화
        req.setBankCode("");     // 빈문자 → null 정규화

        PaymentMethodResponse res = paymentMethodService.edit(1L, 10L, req);

        assertThat(res.getAlias()).isEqualTo("새 별칭");
        // 교차 타입 필드 영향 없음
        assertThat(res.getBankName()).isNull();
        assertThat(res.getBankCode()).isNull();
    }

    @Test
    @DisplayName("CARD: 별칭 중복이면 400")
    void edit_card_alias_duplicate() {
        PaymentMethod entity = cardEntity(10L, member);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(10L, member)).thenReturn(Optional.of(entity));
        when(paymentMethodRepository.existsByMemberAndAliasAndIdNotAndDeletedFalse(member, "중복", 10L))
                .thenReturn(true);

        PaymentMethodEditRequest req = new PaymentMethodEditRequest();
        req.setAlias("중복");

        assertThatThrownBy(() -> paymentMethodService.edit(1L, 10L, req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("CARD: 기본 전환 시 기존 기본 해제")
    void edit_card_switch_default() {
        PaymentMethod entity = cardEntity(10L, member);
        PaymentMethod otherDefault = cardEntity(20L, member);
        otherDefault.setIsDefault(true);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(10L, member)).thenReturn(Optional.of(entity));
        when(paymentMethodRepository.findFirstByMemberAndIsDefaultTrueAndDeletedFalse(member))
                .thenReturn(Optional.of(otherDefault));

        PaymentMethodEditRequest req = new PaymentMethodEditRequest();
        req.setIsDefault(true);

        PaymentMethodResponse res = paymentMethodService.edit(1L, 10L, req);

        assertThat(res.getIsDefault()).isTrue();
        assertThat(otherDefault.getIsDefault()).isFalse(); // 기존 기본 해제 확인
    }

    @Test
    @DisplayName("BANK: 은행 필드만 부분 수정, CARD 필드는 null로 보장")
    void edit_bank_success_updateBankFields() {
        PaymentMethod entity = bankEntity(11L, member);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(11L, member)).thenReturn(Optional.of(entity));
        when(paymentMethodRepository.existsByMemberAndAliasAndIdNotAndDeletedFalse(any(), anyString(), anyLong()))
                .thenReturn(false);

        PaymentMethodEditRequest req = new PaymentMethodEditRequest();
        req.setAlias("월급통장");
        req.setIsDefault(false);
        req.setBankCode("088");
        req.setBankName("신한");
        req.setAcctLast4("9999");

        PaymentMethodResponse res = paymentMethodService.edit(1L, 11L, req);

        assertThat(res.getAlias()).isEqualTo("월급통장");
        assertThat(res.getBankCode()).isEqualTo("088");
        assertThat(res.getBankName()).isEqualTo("신한");
        assertThat(res.getAcctLast4()).isEqualTo("9999");

        assertThat(res.getBrand()).isNull();
        assertThat(res.getLast4()).isNull();
        assertThat(res.getExpMonth()).isNull();
        assertThat(res.getExpYear()).isNull();
    }

    @Test
    @DisplayName("BANK: 교차 타입(CARD) 필드가 값으로 오면 400")
    void edit_bank_reject_cardFields() {
        PaymentMethod entity = bankEntity(11L, member);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(11L, member)).thenReturn(Optional.of(entity));

        PaymentMethodEditRequest req = new PaymentMethodEditRequest();
        req.setBrand("VISA"); // 교차 타입 값

        assertThatThrownBy(() -> paymentMethodService.edit(1L, 11L, req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("BANK: CARD 필드가 빈문자/공백이면 무시(정규화)되어 성공")
    void edit_bank_blank_cardFields_areIgnored() {
        PaymentMethod entity = bankEntity(11L, member);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(11L, member)).thenReturn(Optional.of(entity));
        when(paymentMethodRepository.existsByMemberAndAliasAndIdNotAndDeletedFalse(any(), anyString(), anyLong()))
                .thenReturn(false);

        PaymentMethodEditRequest req = new PaymentMethodEditRequest();
        req.setAlias("새 통장");
        req.setBrand("   "); // 공백 → null 정규화
        req.setLast4("");    // 빈문자 → null 정규화

        PaymentMethodResponse res = paymentMethodService.edit(1L, 11L, req);

        assertThat(res.getAlias()).isEqualTo("새 통장");
        // CARD 쪽은 여전히 null
        assertThat(res.getBrand()).isNull();
        assertThat(res.getLast4()).isNull();
    }

    @Test
    @DisplayName("기본이 아닌 결제수단 삭제 → deleted=true, wasDefault=false, newDefaultId=null")
    void delete_nonDefault_success() {
        // given
        PaymentMethod target = cardEntity(34L, member); // 기본 아님
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(34L, member)).thenReturn(Optional.of(target));

        // when
        PaymentMethodDeleteResponse res = paymentMethodService.deleteAndReport(1L, 34L);

        // then
        assertThat(res.getId()).isEqualTo(34L);
        assertThat(res.isDeleted()).isTrue();
        assertThat(res.isWasDefault()).isFalse();
        assertThat(res.getNewDefaultId()).isNull();

        verify(paymentMethodRepository).delete(target);
        // 기본이 아니므로 승계 조회 호출되지 않음
        verify(paymentMethodRepository, never()).findFirstByMemberAndDeletedFalseOrderByCreateDateDesc(any());
    }

    @Test
    @DisplayName("기본 결제수단 삭제 & 다른 수단 존재 → 최근 생성 수단으로 승계(newDefaultId 설정)")
    void delete_default_withSuccessor_success() {
        // given
        PaymentMethod target = cardEntity(34L, member);
        target.setIsDefault(true); // 기본 수단
        PaymentMethod successor = bankEntity(57L, member); // 승계 대상

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(34L, member)).thenReturn(Optional.of(target));
        when(paymentMethodRepository.findFirstByMemberAndDeletedFalseOrderByCreateDateDesc(member))
                .thenReturn(Optional.of(successor));

        // when
        PaymentMethodDeleteResponse res = paymentMethodService.deleteAndReport(1L, 34L);

        // then
        assertThat(res.getId()).isEqualTo(34L);
        assertThat(res.isDeleted()).isTrue();
        assertThat(res.isWasDefault()).isTrue();
        assertThat(res.getNewDefaultId()).isEqualTo(57L);

        // 승계된 객체가 기본으로 표시되었는지(엔티티 필드 변화) 확인
        assertThat(successor.getIsDefault()).isTrue();

        verify(paymentMethodRepository).delete(target);
        verify(paymentMethodRepository).findFirstByMemberAndDeletedFalseOrderByCreateDateDesc(member);
    }

    @Test
    @DisplayName("기본 결제수단 삭제 & 다른 수단 없음 → 기본 해제(newDefaultId=null)")
    void delete_default_withoutSuccessor_success() {
        // given
        PaymentMethod target = cardEntity(34L, member);
        target.setIsDefault(true); // 기본 수단

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(paymentMethodRepository.findByIdAndMemberAndDeletedFalse(34L, member)).thenReturn(Optional.of(target));
        when(paymentMethodRepository.findFirstByMemberAndDeletedFalseOrderByCreateDateDesc(member))
                .thenReturn(Optional.empty());

        // when
        PaymentMethodDeleteResponse res = paymentMethodService.deleteAndReport(1L, 34L);

        // then
        assertThat(res.getId()).isEqualTo(34L);
        assertThat(res.isDeleted()).isTrue();
        assertThat(res.isWasDefault()).isTrue();
        assertThat(res.getNewDefaultId()).isNull();

        verify(paymentMethodRepository).delete(target);
        verify(paymentMethodRepository).findFirstByMemberAndDeletedFalseOrderByCreateDateDesc(member);
    }
}

