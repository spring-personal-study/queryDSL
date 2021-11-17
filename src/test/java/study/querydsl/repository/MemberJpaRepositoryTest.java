package study.querydsl.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.entity.MemberTeamDto;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    @DisplayName("jpql 문법으로 질의")
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();

        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJpaRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJpaRepository.findByUsername("member1");
        assertThat(result2).isEqualTo(result1);
    }

    @Test
    @DisplayName("queryDsl 문법으로 질의")
    public void basicQueryDSLTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();

        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJpaRepository.findAll_queryDSL();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJpaRepository.findByUsername_queryDSL("member1");
        assertThat(result2).isEqualTo(result1);
    }

    @DisplayName("조건검색 및 멤버 및 팀을 함께 조회하여 성능최적화한 메서드를 테스트")
    @Nested
    public class SearchTest {

        @Test
        @DisplayName("팀명이 teamB이고 나이가 35~40살 사이인 멤버의 팀명과 이름 조회")
        public void searchTest() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");

            em.persist(teamA);
            em.persist(teamB);

            Member member1 = new Member("member1", 10, teamA);
            Member member2 = new Member("member2", 20, teamA);
            Member member3 = new Member("member3", 30, teamB);
            Member member4 = new Member("member4", 40, teamB);
            em.persist(member1);
            em.persist(member2);
            em.persist(member3);
            em.persist(member4);

            MemberSearchCondition condition = new MemberSearchCondition();
            condition.setAgeGoe(35);
            condition.setAgeLoe(40); // age 가 35~40 사이이고
            condition.setTeamName("teamB"); // 팀명이 teamB인

            List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
            assertThat(result)
                    .extracting("username")
                    .containsExactly("member4");
        }

        @Test
        @DisplayName("팀명이 teamB인 멤버의 팀명과 이름 조회")
        public void searchTest2() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");

            em.persist(teamA);
            em.persist(teamB);

            Member member1 = new Member("member1", 10, teamA);
            Member member2 = new Member("member2", 20, teamA);
            Member member3 = new Member("member3", 30, teamB);
            Member member4 = new Member("member4", 40, teamB);
            em.persist(member1);
            em.persist(member2);
            em.persist(member3);
            em.persist(member4);

            MemberSearchCondition condition = new MemberSearchCondition();
            // condition.setAgeGoe(35);
            // condition.setAgeLoe(40);
            condition.setTeamName("teamB"); // 팀명이 teamB인

            List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
            assertThat(result)
                    .extracting("username")
                    .containsExactly("member3", "member4");

            assertThat(result)
                    .extracting("teamName")
                    .containsExactly("teamB", "teamB");
        }

        @Test
        @DisplayName("search 메서드 사용하여 팀명이 teamB이고 나이가 35~40살 사이인 멤버의 팀명과 이름 조회")
        public void searchTest3() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");

            em.persist(teamA);
            em.persist(teamB);

            Member member1 = new Member("member1", 10, teamA);
            Member member2 = new Member("member2", 20, teamA);
            Member member3 = new Member("member3", 30, teamB);
            Member member4 = new Member("member4", 40, teamB);
            em.persist(member1);
            em.persist(member2);
            em.persist(member3);
            em.persist(member4);

            MemberSearchCondition condition = new MemberSearchCondition();
            condition.setAgeGoe(35);
            condition.setAgeLoe(40);
            condition.setTeamName("teamB"); // 팀명이 teamB인

            List<MemberTeamDto> result = memberJpaRepository.search(condition);
            assertThat(result)
                    .extracting("username")
                    .containsExactly("member4");

            assertThat(result)
                    .extracting("teamName")
                    .containsExactly("teamB");
        }
    }


}