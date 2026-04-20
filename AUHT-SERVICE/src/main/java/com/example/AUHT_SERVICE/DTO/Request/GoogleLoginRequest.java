package com.example.AUHT_SERVICE.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "El token de Google es obligatorio")
    @Size(min = 20, message = "El token de Google debe ser válido")
    private String idToken;
}
