package com.aimentor.domain.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record JobPostingUpsertRequest(
        @NotBlank(message = "회사명은 필수입니다.")
        @Size(max = 100, message = "회사명은 100자 이하로 입력해 주세요.")
        String companyName,

        @NotBlank(message = "공고 제목은 필수입니다.")
        @Size(max = 100, message = "공고 제목은 100자 이하로 입력해 주세요.")
        String positionTitle,

        @NotBlank(message = "공고 설명은 필수입니다.")
        @Size(max = 5000, message = "공고 설명은 5000자 이하로 입력해 주세요.")
        String description,

        @Size(max = 500, message = "파일 URL은 500자 이하로 입력해 주세요.")
        String fileUrl,

        @Size(max = 300, message = "채용공고 URL은 300자 이하로 입력해 주세요.")
        String jobUrl,

        LocalDate deadline,

        @Size(max = 50, message = "사이트명은 50자 이하로 입력해 주세요.")
        String siteName
) {
}
