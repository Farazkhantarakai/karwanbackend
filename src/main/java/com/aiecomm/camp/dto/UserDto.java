package com.aiecomm.camp.dto;

import com.aiecomm.camp.entity.AuthProvider;
import com.aiecomm.camp.entity.Role;
import com.aiecomm.camp.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long id;
    private String name;
    private String email;
    private String imageUrl;
    private AuthProvider provider;
    private Role role;

    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
//                .imageUrl(user.getImageUrl())
                .provider(user.getProvider())
                .role(user.getRole())
                .build();
    }
}
