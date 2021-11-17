package study.querydsl.entity;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

/**
 * 조회 최적화용 DTO 추가: 멤버와 팀을 한번에 조회
 */
@Data
public class MemberTeamDto {

    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    @QueryProjection
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
