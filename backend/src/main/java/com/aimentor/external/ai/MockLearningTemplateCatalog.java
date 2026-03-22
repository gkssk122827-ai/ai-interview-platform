package com.aimentor.external.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class MockLearningTemplateCatalog {

    private static final String[] LETTERS = {"A", "B", "C", "D", "E"};
    private static final String SUBJECT_FRONTEND = "프론트엔드";
    private static final String SUBJECT_BACKEND = "백엔드";
    private static final String SUBJECT_FULLSTACK = "풀스택";
    private static final String SUBJECT_DATABASE = "데이터베이스";

    private static final String[] EASY_PROMPTS = {
            "%s의 기본 개념으로 가장 올바른 것은?",
            "%s에 대한 설명 중 정답을 고르세요.",
            "입문 수준에서 %s를 설명할 때 맞는 선택은?"
    };

    private static final String[] MEDIUM_PROMPTS = {
            "실무 상황에서 %s를 적용할 때 가장 적절한 것은?",
            "%s 관련 의사결정으로 올바른 선택은?",
            "%s를 구현하는 과정에서 최선의 방법은?"
    };

    private static final String[] HARD_PROMPTS = {
            "대귨모 트래픽 환경에서 %s를 처리하는 최선의 선택은?",
            "%s에서 트레이드오프를 고려할 때 가장 합리적인 방향은?",
            "%s 문제를 심층 해결할 때 가장 적절한 접근은?"
    };

    private static final Map<String, Template> TEMPLATE_INDEX = buildTemplateIndex();

    private MockLearningTemplateCatalog() {
    }

    static List<Template> select(String subject, String difficulty) {
        String normalizedSubject = subject == null ? "" : subject.trim();
        String normalizedDifficulty = normalizeDifficulty(difficulty);

        if (isSubject(normalizedSubject, SUBJECT_FRONTEND, "frontend")) {
            return frontendTemplates(normalizedDifficulty);
        }
        if (isSubject(normalizedSubject, SUBJECT_BACKEND, "backend")) {
            return backendTemplates(normalizedDifficulty);
        }
        if (isSubject(normalizedSubject, SUBJECT_FULLSTACK, "fullstack")) {
            return fullstackTemplates(normalizedDifficulty);
        }
        if (isSubject(normalizedSubject, SUBJECT_DATABASE, "database", "db")) {
            return databaseTemplates(normalizedDifficulty);
        }
        return backendTemplates(normalizedDifficulty);
    }

    static Template findByQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            return null;
        }
        return TEMPLATE_INDEX.get(question);
    }

    static String findWrongExplanation(String question, String userAnswer) {
        Template template = findByQuestion(question);
        if (template == null || !StringUtils.hasText(userAnswer)) {
            return null;
        }

        String trimmed = userAnswer.trim();
        String exact = template.wrongExplanations().get(trimmed);
        if (exact != null) {
            return exact;
        }

        String normalized = removeLabel(trimmed);
        for (Map.Entry<String, String> entry : template.wrongExplanations().entrySet()) {
            if (removeLabel(entry.getKey()).equalsIgnoreCase(normalized)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String removeLabel(String choice) {
        if (!StringUtils.hasText(choice)) {
            return "";
        }
        String trimmed = choice.trim();
        int dotIndex = trimmed.indexOf('.');
        if (dotIndex > 0 && dotIndex < trimmed.length() - 1) {
            return trimmed.substring(dotIndex + 1).trim();
        }
        return trimmed;
    }

    private static boolean isSubject(String subject, String canonical, String... aliases) {
        if (canonical.equals(subject)) {
            return true;
        }
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(subject)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return "MEDIUM";
        }
        String normalized = difficulty.trim().toUpperCase();
        return switch (normalized) {
            case "EASY", "MEDIUM", "HARD" -> normalized;
            default -> "MEDIUM";
        };
    }

    private static Map<String, Template> buildTemplateIndex() {
        Map<String, Template> index = new LinkedHashMap<>();
        for (String level : List.of("EASY", "MEDIUM", "HARD")) {
            addToIndex(index, frontendTemplates(level));
            addToIndex(index, backendTemplates(level));
            addToIndex(index, fullstackTemplates(level));
            addToIndex(index, databaseTemplates(level));
        }
        return Map.copyOf(index);
    }

    private static void addToIndex(Map<String, Template> index, List<Template> templates) {
        for (Template template : templates) {
            index.put(template.question(), template);
        }
    }

    private static List<Template> frontendTemplates(String difficulty) {
        return switch (difficulty) {
            case "EASY" -> buildTemplates("FRONTEND", "EASY", frontendCards(), EASY_PROMPTS);
            case "HARD" -> buildTemplates("FRONTEND", "HARD", frontendCards(), HARD_PROMPTS);
            default -> buildTemplates("FRONTEND", "MEDIUM", frontendCards(), MEDIUM_PROMPTS);
        };
    }

    private static List<Template> backendTemplates(String difficulty) {
        return switch (difficulty) {
            case "EASY" -> buildTemplates("BACKEND", "EASY", backendCards(), EASY_PROMPTS);
            case "HARD" -> buildTemplates("BACKEND", "HARD", backendCards(), HARD_PROMPTS);
            default -> buildTemplates("BACKEND", "MEDIUM", backendCards(), MEDIUM_PROMPTS);
        };
    }

    private static List<Template> fullstackTemplates(String difficulty) {
        return switch (difficulty) {
            case "EASY" -> buildTemplates("FULLSTACK", "EASY", fullstackCards(), EASY_PROMPTS);
            case "HARD" -> buildTemplates("FULLSTACK", "HARD", fullstackCards(), HARD_PROMPTS);
            default -> buildTemplates("FULLSTACK", "MEDIUM", fullstackCards(), MEDIUM_PROMPTS);
        };
    }

    private static List<Template> databaseTemplates(String difficulty) {
        return switch (difficulty) {
            case "EASY" -> buildTemplates("DATABASE", "EASY", databaseCards(), EASY_PROMPTS);
            case "HARD" -> buildTemplates("DATABASE", "HARD", databaseCards(), HARD_PROMPTS);
            default -> buildTemplates("DATABASE", "MEDIUM", databaseCards(), MEDIUM_PROMPTS);
        };
    }

    private static List<Template> buildTemplates(String category, String difficulty, List<Card> cards, String[] prompts) {
        List<Template> templates = new ArrayList<>();

        for (int cardIndex = 0; cardIndex < cards.size(); cardIndex++) {
            Card card = cards.get(cardIndex);
            for (int promptIndex = 0; promptIndex < prompts.length; promptIndex++) {
                String question = "[" + category + "/" + difficulty + "] " + String.format(prompts[promptIndex], card.concept());

                List<OptionReason> options = new ArrayList<>();
                options.add(new OptionReason(card.correctOption(), card.correctExplanation(), true));
                for (String wrongOption : card.wrongOptions()) {
                    options.add(new OptionReason(wrongOption, "이 보기는 핵심 조건과 맞지 않거나 근거가 부족합니다.", false));
                }

                int shift = (cardIndex + promptIndex) % options.size();
                if (shift > 0) {
                    List<OptionReason> rotated = new ArrayList<>(options.size());
                    for (int idx = 0; idx < options.size(); idx++) {
                        rotated.add(options.get((idx + shift) % options.size()));
                    }
                    options = rotated;
                }

                List<String> choices = new ArrayList<>();
                Map<String, String> wrongExplanations = new LinkedHashMap<>();
                String answer = "";

                for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
                    OptionReason option = options.get(optionIndex);
                    String choice = LETTERS[optionIndex] + ". " + option.text();
                    choices.add(choice);
                    if (option.correct()) {
                        answer = choice;
                    } else {
                        wrongExplanations.put(choice, option.explanation());
                    }
                }

                templates.add(new Template(question, List.copyOf(choices), answer, card.correctOption(), card.correctExplanation(), Map.copyOf(wrongExplanations)));
            }
        }

        return templates;
    }

    private static List<Card> frontendCards() {
        return List.of(
                card("React state 관리", "상태는 불변 업데이트로 변경하고 렌더링 반영을 확인한다.", "React는 참조 변경 기반으로 렌더링을 판단하므로 불변 업데이트가 안전하다.",
                        List.of("상태 객체를 직접 수정해도 동일하다.", "DOM을 직접 조작하면 state가 필요 없다.", "모든 상태를 localStorage에만 저장한다.", "입력마다 새로고침으로 UI를 갱신한다.")),
                card("useEffect 의존성", "effect 내부에서 사용하는 반응형 값을 의존성 배열에 정확히 넣는다.", "의존성 누락은 stale closure를 만들고 동기화 오류를 유발한다.",
                        List.of("성능을 위해 항상 빈 배열만 사용한다.", "경고를 없애기 위해 임의 값만 넣는다.", "effect 로직을 렌더 본문에 넣는다.", "setInterval로만 대체한다.")),
                card("리스트 key 전략", "데이터의 고유하고 안정적인 ID를 key로 사용한다.", "안정적 key는 재정렬/갱신 시 컴포넌트 정체성을 유지한다.",
                        List.of("동적 리스트도 index를 key로 고정한다.", "렌더마다 Math.random()을 key로 사용한다.", "모든 항목에 같은 key를 넣는다.", "리스트가 짧으면 key를 생략한다.")),
                card("코드 스플리팅", "라우트 단위 lazy loading과 dynamic import를 사용한다.", "초기 번들 크기를 줄여 첫 화면 성능과 체감 속도를 개선한다.",
                        List.of("모든 화면을 하나의 번들로 묶는다.", "tree-shaking을 꺼서 문맥을 유지한다.", "이미지를 모두 Base64로 JS에 인라인한다.", "앱 시작 시 모든 기능을 선로딩한다.")),
                card("입력 검색 최적화", "타이핑 기반 API 호출에는 debounce를 적용한다.", "요청 폭주를 줄이고 서버 부하와 레이스를 완화한다.",
                        List.of("키 입력마다 즉시 요청을 보낸다.", "오류가 나면 무한 재시도한다.", "응답 올 때까지 UI를 블로킹한다.", "같은 요청을 병렬로 중복 전송한다.")),
                card("접근성 기본", "label/포커스/시맨틱 요소를 기준으로 폼을 구성한다.", "접근성은 키보드 탐색과 보조기기 호환성을 함께 보장해야 한다.",
                        List.of("placeholder만으로 라벨을 대체한다.", "포커스 아웃라인을 제거한다.", "오류 상태를 색상으로만 전달한다.", "div를 버튼처럼 사용해도 무방하다.")),
                card("XSS 방어", "신뢰할 수 없는 문자열은 이스케이프/검증 후 렌더링한다.", "외부 입력을 그대로 HTML로 렌더링하면 스크립트 주입 위험이 커진다.",
                        List.of("내부 API 응답은 항상 안전하다고 가정한다.", "innerHTML로 바로 렌더링한다.", "localStorage 저장 후 렌더하면 안전하다.", "CSP를 끄면 개발이 쉬워진다."))
        );
    }

    private static List<Card> backendCards() {
        return List.of(
                card("Idempotency", "재시도 가능한 쓰기 요청에는 idempotency key를 설계한다.", "네트워크 재시도 상황에서 중복 처리와 중복 결제를 방지한다.",
                        List.of("POST는 기본적으로 모두 멱등이다.", "성공할 때까지 무한 재시도한다.", "요청 ID를 만들지만 저장하지 않는다.", "중복 쓰기는 DB가 알아서 처리한다.")),
                card("트랜잭션 경계", "일관성이 필요한 최소 작업 단위로 트랜잭션을 짧게 유지한다.", "긴 트랜잭션은 락 경합과 교착 위험을 증가시킨다.",
                        List.of("요청 전체를 하나의 트랜잭션으로 감싼다.", "외부 API 호출 중에도 트랜잭션을 유지한다.", "여러 엔티티 쓰기에도 트랜잭션을 생략한다.", "롤백 설계 없이 수동 재시도한다.")),
                card("N+1 대응", "SQL 관측으로 원인을 확인한 뒤 fetch 전략을 조정한다.", "근거 없이 튜닝하면 다른 병목을 만들 수 있으므로 계측이 우선이다.",
                        List.of("커넥션 풀만 키우면 해결된다.", "모든 연관관계를 즉시로딩으로 바꾼다.", "캐시를 먼저 넣고 본다.", "ORM을 즉시 교체한다.")),
                card("동시성 제어", "충돌 가능 구간에는 optimistic locking(버전 체크)을 적용한다.", "버전 기반 충돌 감지로 데이터 유실을 줄이고 확장성을 유지한다.",
                        List.of("모든 쓰기에 비관적 락을 건다.", "항상 마지막 쓰기 우선으로 둔다.", "버전 체크를 비활성화한다.", "전역 mutex로 직렬화한다.")),
                card("장애 내성", "timeout + 제한된 retry + circuit breaker를 함께 사용한다.", "연쇄 장애를 줄이고 복구 시간을 단축하는 기본 패턴이다.",
                        List.of("timeout 없이 무한 재시도한다.", "의존 서비스 실패여도 성공 응답을 준다.", "불안정할 때 알람을 끈다.", "스레드를 무한히 늘린다.")),
                card("JWT 검증", "서명/만료/issuer/audience를 요청마다 검증한다.", "토큰은 디코딩이 아니라 검증이 핵심이며 claim 검사가 반드시 필요하다.",
                        List.of("payload 디코딩만 하면 충분하다.", "사용자 편의를 위해 만료 검사를 생략한다.", "로그인 시 1회만 검증한다.", "디버깅을 위해 토큰을 로그에 남긴다.")),
                card("페이징", "대량/변동 데이터에는 cursor 기반 페이징을 우선 검토한다.", "offset 기반은 대규모 변경 상황에서 누락/중복이 발생할 수 있다.",
                        List.of("모든 경우 offset만 사용한다.", "전체 데이터를 내려서 클라이언트에서 자른다.", "매 요청 랜덤 정렬을 사용한다.", "페이지 크기 상한 없이 제공한다."))
        );
    }

    private static List<Card> fullstackCards() {
        return List.of(
                card("API 계약 관리", "하위 호환 버전 전략과 폐기 정책을 명확히 운영한다.", "계약 안정성이 프론트/백 동시 배포 리스크를 크게 줄인다.",
                        List.of("응답 스키마를 수시로 바꿔도 된다.", "프론트에서만 보정하면 된다.", "이관 계획 없이 엔드포인트를 복제한다.", "배포 직후 구버전을 즉시 중단한다.")),
                card("CORS 정책", "신뢰 도메인만 명시 허용하고 credentials 정책을 엄격히 관리한다.", "CORS는 보안 경계이므로 최소 권한 설정이 중요하다.",
                        List.of("credentials와 함께 와일드카드를 허용한다.", "운영에서 CORS 검사를 끈다.", "모든 메서드/헤더를 영구 허용한다.", "브라우저 확장에 보안을 맡긴다.")),
                card("추적성", "요청 단위 correlation ID를 프론트-게이트웨이-백엔드로 전파한다.", "교차 레이어 로그 연계가 장애 분석 시간을 단축한다.",
                        List.of("로그 라인마다 랜덤 ID를 따로 쓴다.", "프론트 로그만 남긴다.", "요청 컨텍스트 없이 로그를 모은다.", "디버그 로그를 무제한 저장한다.")),
                card("점진 배포", "feature flag로 단계적 노출과 빠른 롤백 경로를 준비한다.", "일괄 배포보다 장애 범위를 좁히고 복구를 단순화한다.",
                        List.of("로컬 테스트 후 전 사용자 일괄 배포한다.", "빌드 타임 상수로만 기능 온오프한다.", "마이그레이션 스크립트를 플래그로 쓴다.", "전체 배포 후에 모니터링을 켠다.")),
                card("배포 전략", "canary/blue-green + health check를 배포 기본으로 둔다.", "트래픽 제어와 상태 점검이 무중단/안전 배포의 핵심이다.",
                        List.of("운영에 바로 직배포한다.", "배포 속도를 위해 health check를 생략한다.", "모든 인스턴스를 동시에 재시작한다.", "파괴적 스키마 변경과 앱 배포를 한 번에 진행한다.")),
                card("스키마 이관", "하위 호환 마이그레이션 후 애플리케이션 전환을 진행한다.", "expand-contract 순서를 지키면 롤링 배포 중 호환성 문제를 줄인다.",
                        List.of("신규 앱 배포 전 구컬럼을 즉시 삭제한다.", "모든 노드가 동시에 교체된다고 가정한다.", "운영에서 수동 SQL만 수행한다.", "제약조건을 임시 해제 후 복구하지 않는다.")),
                card("E2E 성능 진단", "브라우저 지표, API 지연, DB 지표를 함께 측정 후 튜닝한다.", "한 레이어만 보고 최적화하면 병목을 잘못 진단할 가능성이 높다.",
                        List.of("항상 DB부터 최적화한다.", "프론트 번들만 튜닝한다.", "사용자 제보만으로 병목을 추정한다.", "오버헤드 절감을 위해 관측을 끈다."))
        );
    }

    private static List<Card> databaseCards() {
        return List.of(
                card("인덱스 선택도", "필터에 자주 쓰이고 선택도가 높은 컬럼에 인덱스를 설계한다.", "선택도가 높은 인덱스가 스캔 범위를 효과적으로 줄인다.",
                        List.of("모든 컬럼에 인덱스를 건다.", "저카디널리티 플래그부터 우선 인덱싱한다.", "필터 컬럼은 인덱스를 피한다.", "PK 인덱스만으로 충분하다.")),
                card("복합 인덱스 순서", "조회 패턴의 필터/정렬 우선순위에 맞게 컬럼 순서를 정한다.", "복합 인덱스는 선두 컬럼 규칙 때문에 순서가 성능을 좌우한다.",
                        List.of("복합 인덱스는 순서가 중요하지 않다.", "항상 가장 긴 컬럼을 앞에 둔다.", "컬럼명을 알파벳 순으로 둔다.", "실행계획 확인 없이 생성한다.")),
                card("정규화", "중복과 갱신 이상을 줄이도록 정규화를 적용하고 필요한 경우만 비정규화한다.", "정규화는 데이터 일관성 유지의 기본이며 비정규화는 근거 기반으로 제한해야 한다.",
                        List.of("처음부터 모든 테이블을 비정규화한다.", "조인을 없애기 위해 속성을 무조건 복제한다.", "단순화를 위해 PK를 없앤다.", "다중값을 CSV 문자열로 저장한다.")),
                card("격리수준", "일관성 요구와 동시성 비용을 비교해 격리수준을 선택한다.", "격리수준이 높을수록 이상현상은 줄지만 처리량이 낮아질 수 있다.",
                        List.of("항상 SERIALIZABLE을 사용한다.", "항상 READ UNCOMMITTED를 사용한다.", "읽기 많으면 트랜잭션을 끈다.", "모든 업무에 단일 격리수준을 고정한다.")),
                card("데드락 대응", "락 순서를 일관되게 유지하고 데드락 발생 시 제한 재시도 정책을 둔다.", "예측 가능한 락 순서와 제어된 재시도는 실무 데드락 대응의 핵심이다.",
                        List.of("데드락 방지를 위해 락을 모두 제거한다.", "데드락 시 무한 즉시 재시도한다.", "데드락 에러를 무시한다.", "랜덤 세션을 종료한다.")),
                card("실행계획 분석", "EXPLAIN/ANALYZE로 실제 비용을 확인한 뒤 쿼리/인덱스를 튜닝한다.", "실행계획은 스캔/조인/카디널리티 문제를 찾는 근거다.",
                        List.of("직감으로만 튜닝한다.", "SQL 길이를 줄이는 데 집중한다.", "실행계획은 복잡하니 보지 않는다.", "인덱스 개수만 지표로 본다.")),
                card("참조 무결성", "foreign key로 테이블 간 참조를 강제해 무결성을 보장한다.", "애플리케이션 검증만으로는 모든 경로의 무결성을 보장하기 어렵다.",
                        List.of("유연성을 위해 제약 없이 ID만 저장한다.", "프론트 검증만으로 충분하다.", "주기적 정리 배치로 대체한다.", "고아 데이터를 나중에 수동 정리한다."))
        );
    }

    private static Card card(String concept, String correctOption, String correctExplanation, List<String> wrongOptions) {
        return new Card(concept, correctOption, correctExplanation, wrongOptions);
    }

    record Template(
            String question,
            List<String> choices,
            String answer,
            String shortAnswer,
            String explanation,
            Map<String, String> wrongExplanations
    ) {
    }

    private record OptionReason(String text, String explanation, boolean correct) {
    }

    private record Card(
            String concept,
            String correctOption,
            String correctExplanation,
            List<String> wrongOptions
    ) {
    }
}