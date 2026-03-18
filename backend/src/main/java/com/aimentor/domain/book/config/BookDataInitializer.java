package com.aimentor.domain.book.config;

import com.aimentor.domain.book.entity.Book;
import com.aimentor.domain.book.repository.BookRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seeds a small catalog so the bookstore flow is usable on a fresh local database.
 */
@Configuration
public class BookDataInitializer {

    @Bean
    public ApplicationRunner bookCatalogInitializer(BookRepository bookRepository) {
        return args -> {
            if (bookRepository.count() > 0) {
                return;
            }

            bookRepository.saveAll(List.of(
                    createBook(
                            "백엔드 개발 면접 핵심 가이드",
                            "김개발",
                            "에이멘토 북스",
                            "https://placehold.co/300x420?text=Backend+Interview",
                            "백엔드 면접에서 자주 나오는 질문과 답변 구조를 정리한 실전형 가이드입니다.",
                            24000,
                            30
                    ),
                    createBook(
                            "Spring Boot 실전 설계",
                            "박스프링",
                            "테크로드",
                            "https://placehold.co/300x420?text=Spring+Boot",
                            "Spring Boot, JPA, 트랜잭션, 보안 설계를 실제 서비스 관점에서 정리했습니다.",
                            32000,
                            25
                    ),
                    createBook(
                            "자료구조와 알고리즘 면접 노트",
                            "이알고",
                            "코딩플랜",
                            "https://placehold.co/300x420?text=Algorithm",
                            "코딩 테스트와 기술 면접에 자주 등장하는 핵심 자료구조와 알고리즘을 요약했습니다.",
                            28000,
                            40
                    ),
                    createBook(
                            "합격하는 자기소개서 작성법",
                            "정지원",
                            "커리어랩",
                            "https://placehold.co/300x420?text=Cover+Letter",
                            "지원 동기, 경험 정리, 직무 적합성을 자연스럽게 풀어내는 자기소개서 작성 방법을 담았습니다.",
                            18000,
                            35
                    ),
                    createBook(
                            "AI 시대의 취업 전략",
                            "최미래",
                            "넥스트커리어",
                            "https://placehold.co/300x420?text=Career+Strategy",
                            "AI 활용 학습법, 면접 준비 루틴, 포트폴리오 전략까지 한 번에 정리한 취업 준비서입니다.",
                            26000,
                            20
                    )
            ));
        };
    }

    private Book createBook(
            String title,
            String author,
            String publisher,
            String coverUrl,
            String description,
            int price,
            int stock
    ) {
        return Book.builder()
                .title(title)
                .author(author)
                .publisher(publisher)
                .coverUrl(coverUrl)
                .description(description)
                .price(BigDecimal.valueOf(price))
                .stock(stock)
                .build();
    }
}
