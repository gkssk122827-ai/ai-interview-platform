package com.aimentor.domain.interview.service;

import com.aimentor.domain.interview.entity.InterviewMode;
import java.util.List;
import java.util.Locale;

/**
 * Provides fallback interview-question templates split by mode, job category, and difficulty.
 */
public final class InterviewQuestionCatalog {

    private static final List<InterviewQuestionTemplate> QUESTION_TEMPLATES = List.of(
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "백엔드 팀 프로젝트에서 맡았던 역할과 협업 방식을 구체적으로 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "백엔드 개발자로 일하고 싶은 동기와 그 이유를 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "협업 중 일정이 지연됐을 때 주변과 어떻게 소통했는지 이야기해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "백엔드 업무를 하며 가장 빠르게 성장했다고 느낀 경험을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "API 품질이나 일정 우선순위를 두고 팀과 의견이 달랐던 경험이 있다면 어떻게 조율했는지 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "운영 이슈나 장애 대응 과정에서 실수했던 경험과 이후 어떻게 개선했는지 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "복잡한 백엔드 작업을 맡았을 때 스트레스를 관리하고 끝까지 밀어붙인 방식이 있나요?"),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "다른 직군과 협업하며 요구사항이 자주 바뀌었을 때 어떻게 대응했는지 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "서비스 장애 책임 소재가 불분명한 상황에서 팀 신뢰를 지키며 문제를 해결한 경험이 있나요?"),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "기술적으로 맞다고 생각했지만 조직 상황상 다른 선택을 해야 했던 경험을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "실패 가능성이 큰 과제를 맡았을 때 어떻게 리스크를 관리하고 주변을 설득했는지 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "백엔드 시스템 안정성과 개발 속도 사이의 갈등을 팀 차원에서 풀어낸 경험이 있다면 설명해 주세요."),

            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "프론트엔드 팀 프로젝트에서 맡았던 역할과 협업 방식을 구체적으로 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "프론트엔드 개발자가 되고 싶다고 느낀 계기와 동기를 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "디자인이나 기획과 협업하며 의견 차이를 조율했던 경험을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "사용자 경험 관점에서 본인이 성장했다고 느낀 순간을 이야기해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "UI 품질과 일정 사이에서 우선순위를 결정해야 했던 경험이 있다면 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "배포 후 사용자 불편이 발생했을 때 어떻게 소통하고 후속 조치를 했는지 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "여러 이해관계자의 요구가 충돌할 때 어떤 기준으로 프론트엔드 의사결정을 했나요?"),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "반복적으로 발생한 협업 문제를 개인이나 팀 차원에서 어떻게 개선했는지 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "사용자 경험과 개발 복잡도가 충돌할 때 팀을 설득해 방향을 정한 경험이 있나요?"),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "프론트엔드 품질 이슈로 신뢰가 흔들렸던 상황을 어떻게 회복했는지 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "본인의 UI 판단이 틀렸음을 인지한 뒤 어떻게 수정하고 학습했는지 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.BEHAVIORAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "불명확한 요구사항 아래에서 사용자 중심 의사결정을 이끌어낸 경험을 설명해 주세요."),

            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "Spring Boot에서 트랜잭션이 필요한 상황과 기본 동작을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "REST API 설계 시 HTTP 메서드와 상태 코드를 어떻게 기준 잡는지 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "데이터베이스 인덱스가 필요한 이유와 주의할 점을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "백엔드 개발자 관점에서 캐시를 도입하는 대표적인 이유를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "N+1 문제를 실제로 어떻게 탐지하고 해결할지 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "대량 트래픽 환경에서 API 성능 병목을 진단하는 절차를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "동시성 이슈가 발생하는 시나리오와 이를 완화하는 방법을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "백엔드 서비스에서 예외 처리와 로깅 전략을 어떻게 설계할지 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "모놀리식과 MSA 중 어떤 구조를 선택할지 판단 기준과 트레이드오프를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "읽기 부하가 높은 시스템에서 데이터 일관성과 성능을 함께 관리하는 방식을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "장애 복구까지 고려한 백엔드 아키텍처를 설계한다면 어떤 요소를 우선 보겠습니까?"),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "메시지 큐 기반 비동기 처리 구조를 설계할 때 실패 처리와 재시도 전략을 어떻게 가져갈지 설명해 주세요."),

            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "React에서 state와 props의 차이와 각각의 역할을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "브라우저 렌더링 과정과 reflow, repaint의 차이를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "CSR과 SSR의 차이와 각각이 유리한 상황을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "프론트엔드에서 CORS 문제가 발생하는 이유를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "불필요한 리렌더링을 찾고 줄이는 방법을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "상태 관리 도구를 선택할 때 고려하는 기준과 트레이드오프를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "대규모 화면에서 컴포넌트 구조와 데이터 흐름을 어떻게 설계할지 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "프론트엔드 성능 저하가 발생했을 때 진단 순서와 측정 지표를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "복잡한 인터랙션이 많은 화면에서 렌더링 성능과 유지보수성을 함께 잡는 설계를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "프론트엔드 애플리케이션의 에러 복구 전략과 모니터링 방식을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "접근성과 퍼포먼스를 함께 만족시키는 UI 아키텍처를 어떻게 설계할지 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.TECHNICAL, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "서버 상태와 클라이언트 상태가 복잡하게 얽힌 서비스에서 데이터 일관성을 어떻게 관리할지 말해 주세요."),

            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "백엔드 개발자로서 본인의 강점 하나와 이를 보여준 프로젝트 경험을 함께 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "가장 익숙한 백엔드 기술 스택과 그 기술을 선택한 이유를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "협업 과정에서 맡았던 백엔드 기능과 그 과정에서 배운 점을 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "문제를 해결하며 성장했다고 느낀 백엔드 경험 하나를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "운영 중 발생한 백엔드 이슈를 해결한 경험과 팀 커뮤니케이션 방식을 함께 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "성능 개선이나 트러블슈팅 경험이 있다면 기술적 판단과 협업 과정을 함께 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "백엔드 설계를 바꾸자고 제안했던 경험이 있다면 근거와 결과를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "기술 선택이 팀 일정이나 품질에 영향을 준 사례를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "대규모 트래픽 문제를 해결해야 하는 상황에서 기술적 대응과 팀 조율 방식을 함께 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "시스템 설계상 어려운 트레이드오프를 결정했던 경험이 있다면 조직 맥락까지 포함해 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "백엔드 장애를 겪은 뒤 재발 방지 체계를 만든 경험이 있다면 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "복잡한 기술 문제를 비개발 직군까지 설득하며 추진했던 경험을 설명해 주세요."),

            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "프론트엔드 개발자로서 본인의 강점 하나와 이를 보여준 프로젝트 경험을 함께 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "가장 익숙한 프론트엔드 기술 스택과 그 기술을 선택한 이유를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "협업 과정에서 맡았던 화면이나 기능과 그 과정에서 배운 점을 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "사용자 경험을 개선하며 성장했다고 느낀 경험을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "프론트엔드 이슈를 해결한 경험과 디자인 또는 기획과의 협업 방식을 함께 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "렌더링 성능 개선 경험이 있다면 기술적 판단과 팀 커뮤니케이션을 함께 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "컴포넌트 구조나 상태 관리를 개선하자고 제안했던 경험을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "사용자 피드백을 반영해 기능 방향을 수정했던 경험을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "대규모 화면 성능 문제를 해결할 때 기술적 대응과 팀 조율을 함께 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "복잡한 UI 요구사항과 일정 압박 사이에서 의사결정했던 경험을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "프론트엔드 장애나 품질 이슈 이후 재발 방지 체계를 만든 경험이 있다면 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.COMPREHENSIVE, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "접근성, 성능, 비즈니스 요구를 함께 만족시키는 방향으로 팀을 설득한 경험을 설명해 주세요."),

            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "이력서나 지원서에 적은 백엔드 프로젝트 중 가장 자신 있는 경험 하나를 소개해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "지원서에 적은 기술 스택 중 실제로 가장 깊게 사용해 본 기술과 사용 맥락을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "작성한 경험 중 본인이 직접 기여한 부분과 팀의 기여를 구분해서 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.EASY, "지원서에 적은 성과 중 가장 의미 있다고 보는 항목과 이유를 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "이력서에 적은 백엔드 성능 개선 경험에서 실제 병목 원인과 본인 판단 근거를 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "지원서에 적은 트러블슈팅 사례에서 문제 원인 분석 과정을 단계별로 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "프로젝트 설명에 적은 아키텍처 선택이 왜 적절했는지 당시 제약 조건까지 포함해 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.MEDIUM, "지원서의 협업 경험 중 갈등이 있었던 사례를 하나 골라 사실 관계 중심으로 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "이력서의 백엔드 성과 수치가 어떻게 측정된 것인지 지표와 검증 방법까지 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "지원서에 적은 핵심 역할이 본인 주도였음을 보여주는 의사결정 장면을 구체적으로 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "프로젝트 소개에 적은 기술적 성공과 별개로 아쉬웠던 선택이 있다면 근거와 함께 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.BACKEND, InterviewQuestionDifficulty.HARD, "경력 또는 프로젝트 설명 중 가장 검증받고 싶은 항목 하나를 골라 깊이 있게 설명해 주세요."),

            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "이력서나 지원서에 적은 프론트엔드 프로젝트 중 가장 자신 있는 경험 하나를 소개해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "지원서에 적은 기술 스택 중 실제로 가장 깊게 사용해 본 프론트엔드 기술과 사용 맥락을 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "작성한 경험 중 본인이 직접 기여한 화면이나 기능을 구체적으로 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.EASY, "지원서에 적은 성과 중 사용자 관점에서 가장 의미 있는 항목과 이유를 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "이력서에 적은 프론트엔드 성능 개선 경험에서 병목을 어떻게 확인했는지 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "지원서에 적은 UI 개선 사례에서 사용자 문제를 어떻게 정의했고 무엇을 바꿨는지 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "프로젝트 설명에 적은 상태 관리나 구조 설계 선택이 왜 적절했는지 당시 제약과 함께 말해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.MEDIUM, "지원서의 협업 경험 중 디자인 또는 기획과 조율했던 사례를 구체적으로 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "이력서의 성과 수치가 실제 사용자 경험 개선으로 이어졌다는 근거를 어떻게 확인했는지 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "지원서에 적은 핵심 역할이 본인 주도였음을 보여주는 의사결정 장면을 구체적으로 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "프로젝트 설명 중 가장 검증받고 싶은 UI 또는 구조적 결정 하나를 골라 깊이 있게 설명해 주세요."),
            new InterviewQuestionTemplate(InterviewMode.RESUME_BASED, InterviewQuestionCategory.FRONTEND, InterviewQuestionDifficulty.HARD, "경험 소개에서 드러나지 않은 실패나 한계가 있다면 무엇이었고 어떻게 보완했는지 설명해 주세요.")
    );

    private InterviewQuestionCatalog() {
    }

    public static InterviewQuestionCategory resolveCategory(String positionTitle, String jobPostingSnapshot) {
        String combinedText = ((positionTitle == null ? "" : positionTitle) + " " + (jobPostingSnapshot == null ? "" : jobPostingSnapshot))
                .toLowerCase(Locale.ROOT);

        if (combinedText.contains("front")
                || combinedText.contains("react")
                || combinedText.contains("ui")
                || combinedText.contains("프론트")
                || combinedText.contains("frontend")) {
            return InterviewQuestionCategory.FRONTEND;
        }

        return InterviewQuestionCategory.BACKEND;
    }

    public static InterviewQuestionDifficulty resolveDifficulty(int sequenceNumber, int totalQuestionCount) {
        if (totalQuestionCount <= 1) {
            return InterviewQuestionDifficulty.MEDIUM;
        }

        int ratio = Math.round(sequenceNumber * 100.0f / totalQuestionCount);
        if (ratio <= 34) {
            return InterviewQuestionDifficulty.EASY;
        }
        if (ratio <= 67) {
            return InterviewQuestionDifficulty.MEDIUM;
        }
        return InterviewQuestionDifficulty.HARD;
    }

    public static List<String> findQuestions(
            InterviewMode mode,
            InterviewQuestionCategory category,
            InterviewQuestionDifficulty difficulty
    ) {
        List<String> exactMatches = QUESTION_TEMPLATES.stream()
                .filter(template -> template.mode() == mode
                        && template.category() == category
                        && template.difficulty() == difficulty)
                .map(InterviewQuestionTemplate::questionText)
                .toList();

        if (!exactMatches.isEmpty()) {
            return exactMatches;
        }

        return QUESTION_TEMPLATES.stream()
                .filter(template -> template.mode() == mode && template.category() == category)
                .map(InterviewQuestionTemplate::questionText)
                .distinct()
                .toList();
    }

    public enum InterviewQuestionCategory {
        BACKEND,
        FRONTEND
    }

    public enum InterviewQuestionDifficulty {
        EASY,
        MEDIUM,
        HARD
    }

    public record InterviewQuestionTemplate(
            InterviewMode mode,
            InterviewQuestionCategory category,
            InterviewQuestionDifficulty difficulty,
            String questionText
    ) {
    }
}
