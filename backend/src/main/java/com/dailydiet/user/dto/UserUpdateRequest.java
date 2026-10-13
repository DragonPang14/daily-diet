package com.dailydiet.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UserUpdateRequest {

    @NotNull
    @Min(value = 0, message = "性别 0=女 1=男")
    @Max(value = 1)
    private Integer gender;

    @NotNull
    private LocalDate birthday;

    @NotNull
    @DecimalMin("120.0")
    @DecimalMax("230.0")
    private BigDecimal height;

    @NotNull
    @DecimalMin("30.0")
    @DecimalMax("200.0")
    private BigDecimal currentWeight;

    private BigDecimal targetWeight;
}
