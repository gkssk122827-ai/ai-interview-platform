package com.aimentor.domain.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobPostingUrlRegisterRequest(
        @NotBlank(message = "채용공고 URL은 필수입니다.")
        @Size(max = 300, message = "채용공고 URL은 300자 이하로 입력해 주세요.")
        String url
) {
}
