package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;
import study.querydsl.repository.MemberRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberTeamV1(MemberSearchCondition condition) {
        return memberJpaRepository.search(condition);
    }

    // http://localhost:8080/v2/members?page=0&size=5
    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberTeamV2(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageSimple(condition, pageable);
    }

    @GetMapping("/v3/members")
    public Page<MemberTeamDto> searchMemberTeamV3(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageComplex(condition, pageable);
    }

    // http://localhost:8080/v2/members?page=0&size=110
    // size가 110개이면 count 실행하지 않음 (전체 데이터가 100개밖에 없으므로)
    // http://localhost:8080/v2/members?page=0&size=99
    // size가 99개면 count 실행
    @GetMapping("/v4/members")
    public Page<MemberTeamDto> searchMemberTeamV4(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageComplexQueryCountWhenItNeeded(condition, pageable);
    }

}
