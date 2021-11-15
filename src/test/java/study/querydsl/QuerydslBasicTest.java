package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    //JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        //queryFactory = new JPAQueryFactory(em);

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

        // 추가 데이터 삽입
        em.persist(new Member(null, 98));
        em.persist(new Member("member5", 99));
        em.persist(new Member("member6", 100));

        // 초기화
        em.flush(); // 영속성 컨텍스트에 쿼리 날리기
        em.clear(); // 1차 캐시 삭제
        /*
        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        members.forEach(System.out::println);
        members.forEach(member -> System.out.println(member.getTeam()));

         */
    }

    @Test
    @Transactional
    public void testStartJPQL() {
        // member1을 찾아라.
        Member result = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(result.getUsername()).isEqualTo("member1");

    }

    @Test
    @Transactional
    public void testStartQueryDSL() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember m = QMember.member;

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @Transactional
    public void search() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        List<Member> findMember = queryFactory.selectFrom(member)
                .where(member.username.in("member1", "member2")
                        .and(member.age.loe(30))).fetch();

        assertThat(findMember.size()).isEqualTo(2);

    }

    @Test
    @Transactional
    public void search2() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        List<Member> findMember = queryFactory.selectFrom(member)
                .where(member.username.like("member%"))
                .fetch();

        assertThat(findMember.size()).isEqualTo(6);

    }

    @Test
    @Transactional
    public void search3() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        List<Member> findMember = queryFactory.selectFrom(member)
                .where(member.username.endsWith("member"))
                .fetch();

        assertThat(findMember.size()).isEqualTo(0);
    }

    @Test
    @Transactional
    public void search4() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        List<Member> findMember = queryFactory.selectFrom(member)
                .where(
                        member.username.startsWith("member"),
                        member.age.between(10, 30)
                )
                .fetch();

        assertThat(findMember.size()).isEqualTo(3);
    }


    @Test
    @Transactional
    public void resultFetch() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        // 여러 건 (List) 조회 // 결과가 없으면 빈 리스트 반환
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        // 단 건 조회 // 결과없으면 null 반환, 결과가 두개 이상이면 NonUniqueResultException 발생
        Member findMember1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // 처음 한 건 조회
        Member findMember2 = queryFactory
                .selectFrom(member)
                .fetchFirst(); // .limit(1).fetchOne() 과 동일

        // 페이징에서 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        // count 쿼리로 변경 (검색 결과 갯수 조회)
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();

        assertThat(fetch.size()).isEqualTo(7);
        assertThat(findMember2.getUsername()).isEqualTo("member1");
        assertThat(findMember2.getUsername()).isEqualTo("member1");
        assertThat(results.getTotal()).isEqualTo(7);
        assertThat(count).isEqualTo(7);
    }


    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (desc)
     * 2. 회원 이름 올림차순 (asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    @Transactional
    public void sort() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(98))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertThat(result.get(0).getUsername()).isEqualTo("member6"); // age= 100
        assertThat(result.get(1).getUsername()).isEqualTo("member5"); // age= 99
        assertThat(result.get(2).getUsername()).isNull(); // age= 98

    }


    @Test
    @Transactional
    public void paging1() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        List<Member> result = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @Transactional
    public void paging2() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(7);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    @Transactional
    public void aggregation() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(7);
        assertThat(tuple.get(member.age.sum())).isEqualTo(397);
        assertThat(tuple.get(member.age.avg()).intValue()).isEqualTo(56);
        assertThat(tuple.get(member.age.max())).isEqualTo(100);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    @Transactional
    public void group() throws Exception {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QTeam team = QTeam.team;
        QMember member = QMember.member;

        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    @Transactional
    public void join() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QTeam team = QTeam.team;
        QMember member = QMember.member;

        List<Member> result = queryFactory.selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

    }

    /**
     * 세타 조인 (연관관계가 없는 두 테이블 간의 조인) - 카테시안 조인
     * 아래 코드로 세타 조인 수행시 outer join (외부 조인) 수행 불가, on을 사용해야 함
     * <p>
     * 회원의 이름이 팀 이름과 같은 회원을 조회
     */
    @Test
    @Transactional
    public void theta_join() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        em.persist(new Member("teamA", 30));
        em.persist(new Member("teamB", 30));
        em.persist(new Member("teamC", 30));

        QTeam team = QTeam.team;
        QMember member = QMember.member;

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result.size()).isEqualTo(2);
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인해라. 그리고 회원은 모두 조회
     * jpql: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    @Transactional
    public void join_on_filtering() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QTeam team = QTeam.team;
        QMember member = QMember.member;

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 회원의 경우에는 팀 정보를 가져와라
     */
    @Test
    @Transactional
    public void join_on_no_relation() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        em.persist(new Member("teamA", 30));
        em.persist(new Member("teamB", 30));
        em.persist(new Member("teamC", 30));

        QTeam team = QTeam.team;
        QMember member = QMember.member;

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    /**
     * 페치 조인 미적용
     */
    @Test
    @Transactional
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QTeam team = QTeam.team;
        QMember member = QMember.member;

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 초기화가 되었는가?
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    /**
     * 페치 조인 적용
     */
    @Test
    @Transactional
    public void fetchJoinYes() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QTeam team = QTeam.team;
        QMember member = QMember.member;

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }


    /**
     * 나이가 가장 많은 회원 조회 (서브쿼리 없이도 되지만.)
     */
    @Test
    @Transactional
    public void subQuery() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        em.persist(new Member("teamA", 999));

        QMember member = QMember.member;
        QMember memberSub = new QMember("memberSub");

        Member oldestPerson = queryFactory.selectFrom(member)
                                    .from(member)
                                    .where(member.age.eq(
                                            JPAExpressions.select(memberSub.age.max())
                                                          .from(memberSub)
                                    ))

                                    .fetchOne();

        assertThat(oldestPerson.getAge()).isEqualTo(999);
    }

    /**
     * 나이가 평균보다 많은 회원 조회
     */
    @Test
    @Transactional
    public void subQuery2() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;
        QMember memberSub = new QMember("memberSub"); // 셀프 조인시 서브쿼리의 member 테이블과 분리해야 하므로 지정

        List<Member> result = queryFactory.select(member)
                .from(member)
                .where(member.age.gt(
                        JPAExpressions
                        .select(memberSub.age.avg())
                        .from(memberSub)
                ))
                .groupBy(member)
                .fetch();

        result.forEach(e -> System.out.println("result:: \n" + e + ":: \n"));

        assertThat(result)
                .extracting("age")
                .containsExactlyInAnyOrder(98, 99, 100);
    }


    /**
     * 나이가 평균보다 많은 회원 조회2
     */
    @Test
    @Transactional
    public void subQuery3() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory.select(member, JPAExpressions
                                                                .select(member.age.avg()
                                                                                  .intValue())
                                                                .from(member)
                                        )
                                        .from(member)
                                        .where(member.age.gt(JPAExpressions
                                                .select(memberSub.age.avg())
                                                .from(memberSub)))
                                        .groupBy(member)
                                        .fetch();

        result.forEach(e -> System.out.println("result:: " + e + " :: \n"));
        List<Member> memberList = result.stream()
                                        .map(m -> m.get(0, Member.class))
                                        .collect(Collectors.toList());

        int avgAge = result.stream()
                              .map(m -> m.get(1, Integer.class))
                              .findFirst()
                              .orElseThrow(NullPointerException::new);

        assertThat(memberList)
                .extracting("age")
                .allMatch(age -> (int) age > avgAge);
    }

    /**
     * 회원의 나이별 조회
     */
    @Test
    @Transactional
    public void caseTest() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    /**
     * 복잡한 case 문
     */
    @Test
    @Transactional
    public void complexCaseTest() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 40)).then("21~40살")
                        .when(member.age.eq(100)).then("100살")
                        .otherwise("기타"))
                        .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    /**
     * 상수 출력
     */
    @Test
    @Transactional
    public void constantTest() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    /**
     * username_age 출력
     */
    @Test
    @Transactional
    public void concat() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }
}
