package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.hibernate.dialect.H2Dialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDTO;
import study.querydsl.dto.QMemberDTO;
import study.querydsl.dto.UserDTO;
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

    @Test
    @Transactional
    public void simpleProjection() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<String> result = queryFactory.select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @Transactional
    public void tupleProjection() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<Tuple> result = queryFactory.select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple t : result) {
            System.out.println("member.username = " + t.get(member.username));
            System.out.println("member.age = " + t.get(member.age));
        }
    }

    /**
     * MemberDTO 로 테이블 값 가져오기 (JPQL)
     */
    @Test
    @Transactional
    public void findDtoBYJPQL() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<MemberDTO> query = em.createQuery(
                        "SELECT new study.querydsl.dto.MemberDTO(m.username, m.age)" + // 마치 생성자처럼 사용
                                "FROM Member m", MemberDTO.class)
                .getResultList();

        for (MemberDTO dto : query) {
            System.out.println("memberDto = " + dto);
        }
    }

    /**
     * MemberDTO 로 테이블 값 가져오기 (QueryDSL)
     */
    @Test
    @Transactional
    public void findDtoBYQueryDSL() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<MemberDTO> result = queryFactory
                .select(Projections.bean(MemberDTO.class, // bean 은 기본생성자 반드시 필요
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDTO dto : result) {
            System.out.println("memberDto = " + dto);
        }
    }

    @Test
    @Transactional
    public void findDtoBYConstructor() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<MemberDTO> result = queryFactory
                .select(Projections.constructor(MemberDTO.class, // constructor 는 기본생성자 필요없음 (대신 생성자 인자값을 맞춰줘야함)
                        // 인자값을 다르게 주어도 컴파일레벨에서 에러를 잡지 못함
                        // (컴파일레벨에서 에러를 잡는 방법에 대해서는 아래 findDTOByQueryProjection 테스트를 참고)
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDTO dto : result) {
            System.out.println("memberDto = " + dto);
        }
    }

    @Test
    @Transactional
    public void findDtoBYField() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<MemberDTO> result = queryFactory
                .select(Projections.fields(MemberDTO.class, // fields 는 getter, setter, 기본생성자 필요, 생성자 인자값 안맞춰줘도 됨)
                        member.age,
                        member.username))
                .from(member)
                .fetch();
        for (MemberDTO dto : result) {
            System.out.println("memberDto = " + dto);
        }
    }

    @Test
    @Transactional
    public void findDtoBYUserDTO() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<UserDTO> result = queryFactory
                .select(Projections.fields(UserDTO.class,
                        member.age.as("userAge"),
                        member.username.as("name"))) // as가 없으면 (매칭되는 필드가 없어서) null 값이 들어옴.
                .from(member)
                .fetch();
        for (UserDTO dto : result) {
            System.out.println("memberDto = " + dto);
        }
    }

    @Test
    @Transactional
    public void findDtoBYUserDTOUsingSubquery() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        QMember memberSub = new QMember("memberSub");

        List<UserDTO> result = queryFactory
                .select(Projections.fields(UserDTO.class,
                        ExpressionUtils // 서브쿼리시 필드 매칭은 ExpressionUtils 사용 외에는 방법이 없음
                                .as(JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub), "userAge"), // 서브쿼리 사용시 as userAge 매칭
                        member.username.as("name"))) // as가 없으면 (매칭되는 필드가 없어서) null 값이 들어옴.
                .from(member)
                .fetch();
        for (UserDTO dto : result) {
            System.out.println("memberDto = " + dto);
        }
    }

    @Test
    @Transactional
    public void findDtoBYUserDTOConstructor() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<UserDTO> result = queryFactory
                .select(Projections.constructor(UserDTO.class, // constructor 는 기본생성자 필요없음 (대신 생성자 인자값을 맞춰줘야함)
                        // 인자값을 다르게 주어도 컴파일레벨에서 에러를 잡지 못함
                        // (컴파일레벨에서 에러를 잡는 방법에 대해서는 아래 findDTOByQueryProjection 테스트를 참고)
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (UserDTO dto : result) {
            System.out.println("userDTO = " + dto);
        }
    }

    @Test
    @Transactional
    public void findDTOByQueryProjection() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        List<MemberDTO> result = queryFactory
                .select(new QMemberDTO(member.username, member.age)) // 사용하기 위해서는 MemberDTO 의 생성자에 @QueryProjection 설정이 필요.
                // 컴파일 시간에 오류 검출가능 (가장 안전한 방법)
                // 단점:  1. @QueryProjection 이 작성이 필요함
                //       2. MemberDTO 객체가 queryDSL 에 의존성을 가지게 됨 (결합도 증가),
                //          다시 말해 순수객체가 되지 못함
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    /**
     * 동적 쿼리 해결 : BooleanBuilder 사용
     */
    @Test
    @Transactional
    public void dynamicQuery_BooleanBuilder() {

        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);

        assertThat(result)
                .extracting("username")
                .containsExactly("member1");
    }

    private List<Member> searchMember1(String usernameCondition, Integer ageCondition) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        // BooleanBuilder 사용
        BooleanBuilder builder = new BooleanBuilder();

        if (usernameCondition != null) {
            builder.and(member.username.eq(usernameCondition));
        }

        if (ageCondition != null) {
            builder.and(member.age.eq(ageCondition));
        } // ageCondition 은 null 이므로 where member.age = 10 조건은 빠지게 된다.

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 동적 쿼라 : 다중 where 파라매터 사용
     */
    @Test
    @Transactional
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }


    private List<Member> searchMember2(String usernameCondition, Integer ageCondition) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        return queryFactory
                .selectFrom(member)
                // .where(allEq(usernameCondition, ageCondition)) // 조립가능한 방식(컴포지션)
                .where(usernameEq(usernameCondition), ageEq(ageCondition)) // 조금더 가독성이 향상됨.
                // (where 조건에 null 이 있으면 무시되는 특성을 이용함)
                // 그러나 모든 조건이 null 이 되면 모든 row 를 조회할 위험성이 있음.
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCondition) {
        QMember member = QMember.member;
        return usernameCondition == null ? null : member.username.eq(usernameCondition);
    }

    private BooleanExpression ageEq(Integer ageCondition) {
        QMember member = QMember.member;
        return ageCondition == null ? null : member.age.eq(ageCondition);
    }

    private BooleanExpression allEq(String usernameCondition, Integer ageCondition) {
        return usernameEq(usernameCondition).and(ageEq(ageCondition));
    }

    /**
     * 벌크 연산
     */
    @Test
    @Transactional
    public void bulkUpdate() {
        // member1 = 10 -> 비회원
        // member2 = 20 -> 비회원
        // member3 = 30 -> 유지
        // member4 = 40 -> 유지
        // 주의점:

        /*
        벌크연산은 영속성 컨텍스트를 무시하고 진행된다.
        따라서 영속성 컨텍스트에는 아직
        member1 = 10 -> member1
        member2 = 20 -> member2
        member3 = 30 -> member3
        member4 = 40 -> member4
        로서
        다시 말해 벌크 연산전의 값을 가지고 있으므로 실제 데이터베이스의 값과 영속성 컨텍스트의 값이 달라진 상태가 된다.
        따라서 벌크 연산 후에는 반드시 영속성 컨텍스트를 플러시 및 비워줘야 한다.
         */

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear(); // 벌크 연산 후 반드시 수행, 안하면 select 를 해도 실제 데이터베이스의 값을 가져오지 않는다.
                    // (영속성 컨텍스트의 1차 캐시에 있는 값을 가져온다.)

        assertThat(count).isEqualTo(2);
    }

    /**
     * 벌크 연산 : 수정 삭제
     */
    @Test
    @Transactional
    public void bulkOperation() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();

        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        em.flush();
        em.clear();
    }

    /**
     * member 테이블에서 username 을 M 이라는 이름으로 바꿔서 출력
     *
     * 결과:
     * username                username(변경후)
     * ----------       ->     -------------
     * member1                 M1
     * member2                 M2
     * member3                 M3
     * ....                    .....
     */
    @Test
    @Transactional
    public void sqlFunction() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})", // function 명(replace 등)은 H2Dialect 클래스 등에 등록되어 있는 함수여야 한다.
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @Transactional
    public void sqlFunction2() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;

        List<String> result = queryFactory
                .select(member.username.upper())
                .from(member)
                .where(member.username.eq(member.username.lower())) // 표준 ansi 함수들은 굳이 function 을 호출하지 않아도 기본 제공한다.
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
























