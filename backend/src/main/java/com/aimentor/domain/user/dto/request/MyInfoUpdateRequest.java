package com.aimentor.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MyInfoUpdateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하로 입력해 주세요.")
        String name,

        @NotBlank(message = "전화번호는 필수입니다.")
        @Size(max = 20, message = "전화번호는 20자 이하로 입력해 주세요.")
        String phone
) {
}
