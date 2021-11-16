package study.querydsl.dto;

import lombok.Data;

@Data
public class UserDTO {

    private String name;
    private int userAge;

    public UserDTO() {}

    public UserDTO(String name, int userAge) {
        this.name = name;
        this.userAge = userAge;
    }
}
