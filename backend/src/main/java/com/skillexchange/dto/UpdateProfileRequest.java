package com.skillexchange.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateProfileRequest {
    @Size(max = 120)
    private String fullName;

    @Size(max = 20)
    @Pattern(regexp = "^[+0-9() .-]*$", message = "Phone number contains invalid characters")
    private String phone;

    @Size(max = 1000)
    private String bio;

    @Size(max = 120)
    private String location;

    @Size(max = 500)
    private String profilePictureUrl;

    @Size(max = 200)
    private String languages;

    @Size(max = 50)
    private String communicationLanguage;

    @Min(0)
    @Max(80)
    private Integer experienceYears;
}
