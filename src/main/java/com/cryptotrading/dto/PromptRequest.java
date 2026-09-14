package com.cryptotrading.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromptRequest {

    @NotBlank(message = "Prompt must not be empty")
    public  String prompt;
}
