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
 * Stores a user's resume used for interview preparation.
 */
@Getter
@Entity
@Table(name = "resumes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resume extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(length = 500)
    private String fileUrl;

    @Builder
    public Resume(User user, String title, String content, String fileUrl) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.fileUrl = fileUrl;
    }

    public void update(String title, String content, String fileUrl) {
        this.title = title;
        this.content = content;
        this.fileUrl = fileUrl;
    }
}
