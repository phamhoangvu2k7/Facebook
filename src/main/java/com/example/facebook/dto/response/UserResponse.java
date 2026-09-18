package com.example.facebook.dto.response;

import com.example.facebook.enums.Gender;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String avatarUrl;
    private String bio;
    private LocalDateTime createdAt;
}
