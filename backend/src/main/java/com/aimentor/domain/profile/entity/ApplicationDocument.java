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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stores a unified application document that can contain resume and cover-letter content.
 */
@Getter
@Entity
@Table(name = "application_documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationDocument extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 5000)
    private String resumeText;

    @Column(length = 5000)
    private String coverLetterText;

    @Column(length = 255)
    private String originalFileName;

    @Column(length = 500)
    private String storedFilePath;

    @Column(length = 500)
    private String fileUrl;

    @Builder
    public ApplicationDocument(
            User user,
            String title,
            String resumeText,
            String coverLetterText,
            String originalFileName,
            String storedFilePath,
            String fileUrl
    ) {
        this.user = user;
        this.title = title;
        this.resumeText = resumeText;
        this.coverLetterText = coverLetterText;
        this.originalFileName = originalFileName;
        this.storedFilePath = storedFilePath;
        this.fileUrl = fileUrl;
    }

    public void updateTextContent(String title, String resumeText, String coverLetterText) {
        this.title = title;
        this.resumeText = resumeText;
        this.coverLetterText = coverLetterText;
    }

    public void updateFileMetadata(String originalFileName, String storedFilePath, String fileUrl) {
        this.originalFileName = originalFileName;
        this.storedFilePath = storedFilePath;
        this.fileUrl = fileUrl;
    }
}
