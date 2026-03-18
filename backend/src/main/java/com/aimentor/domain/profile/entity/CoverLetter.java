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

@Getter
@Entity
@Table(name = "cover_letters")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoverLetter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 5000)
    private String content;

    @Builder
    public CoverLetter(User user, String title, String companyName, String content) {
        this.user = user;
        this.title = title;
        this.companyName = companyName;
        this.content = content;
    }

    public void update(String title, String companyName, String content) {
        this.title = title;
        this.companyName = companyName;
        this.content = content;
    }
}
