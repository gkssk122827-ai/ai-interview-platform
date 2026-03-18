package com.aimentor.domain.profile.entity;

import com.aimentor.common.entity.BaseTimeEntity;
import com.aimentor.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stores a job posting selected by the user for interview context.
 */
@Getter
@Entity
@Table(name = "job_postings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPosting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 100)
    private String positionTitle;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(length = 500)
    private String fileUrl;

    @Column(length = 300)
    private String jobUrl;

    private LocalDate deadline;

    @Builder
    public JobPosting(
            User user,
            String companyName,
            String positionTitle,
            String description,
            String fileUrl,
            String jobUrl,
            LocalDate deadline
    ) {
        this.user = user;
        this.companyName = companyName;
        this.positionTitle = positionTitle;
        this.description = description;
        this.fileUrl = fileUrl;
        this.jobUrl = jobUrl;
        this.deadline = deadline;
    }

    public void update(
            String companyName,
            String positionTitle,
            String description,
            String fileUrl,
            String jobUrl,
            LocalDate deadline
    ) {
        this.companyName = companyName;
        this.positionTitle = positionTitle;
        this.description = description;
        this.fileUrl = fileUrl;
        this.jobUrl = jobUrl;
        this.deadline = deadline;
    }
}
