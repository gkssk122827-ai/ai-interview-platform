export const stubQuestionTemplates = {
  BEHAVIORAL: {
    BACKEND: {
      EASY: [
        { category: '협업', questionText: '백엔드 팀 프로젝트에서 본인이 맡았던 역할과 협업 방식을 설명해 주세요.' },
        { category: '동기', questionText: '백엔드 개발 직무를 선택한 이유와 계속 성장하고 싶은 방향을 말해 주세요.' },
        { category: '성장', questionText: '백엔드 업무를 하며 가장 빠르게 성장했다고 느낀 경험을 설명해 주세요.' },
      ],
      MEDIUM: [
        { category: '갈등', questionText: 'API 우선순위나 구현 방식으로 의견 충돌이 있었을 때 어떻게 조율했는지 설명해 주세요.' },
        { category: '실패', questionText: '운영 이슈나 장애 대응 중 실수했던 경험과 이후 개선한 점을 말해 주세요.' },
        { category: '협업', questionText: '요구사항이 자주 바뀌는 상황에서 팀과 어떻게 소통하며 대응했는지 설명해 주세요.' },
      ],
      HARD: [
        { category: '책임감', questionText: '서비스 장애 책임 소재가 불분명한 상황에서 팀 신뢰를 지키며 문제를 해결한 경험이 있나요?' },
        { category: '의사결정', questionText: '기술적으로 맞다고 생각했지만 조직 상황상 다른 선택을 해야 했던 경험을 설명해 주세요.' },
      ],
    },
    FRONTEND: {
      EASY: [
        { category: '협업', questionText: '프론트엔드 팀 프로젝트에서 맡았던 역할과 협업 방식을 설명해 주세요.' },
        { category: '동기', questionText: '프론트엔드 개발 직무를 선택한 이유와 사용자 경험에 관심을 갖게 된 계기를 말해 주세요.' },
        { category: '성장', questionText: '프론트엔드 업무를 하며 가장 크게 성장했다고 느낀 경험을 설명해 주세요.' },
      ],
      MEDIUM: [
        { category: '갈등', questionText: '디자인이나 기획과 의견 차이가 있었을 때 어떤 기준으로 조율했는지 설명해 주세요.' },
        { category: '실패', questionText: '배포 후 UI 문제로 사용자 불편이 발생했을 때 어떻게 대응했는지 말해 주세요.' },
        { category: '협업', questionText: '여러 이해관계자의 요구가 충돌할 때 어떤 기준으로 프론트엔드 결정을 했는지 설명해 주세요.' },
      ],
      HARD: [
        { category: '책임감', questionText: '품질 이슈로 신뢰가 흔들렸던 상황을 어떻게 회복했는지 설명해 주세요.' },
        { category: '의사결정', questionText: '사용자 경험과 개발 복잡도가 충돌할 때 팀을 설득해 방향을 정한 경험이 있나요?' },
      ],
    },
  },
  TECHNICAL: {
    BACKEND: {
      EASY: [
        { category: '기술', questionText: 'Spring Boot에서 트랜잭션이 필요한 상황과 기본 동작을 설명해 주세요.' },
        { category: '기술', questionText: 'REST API 설계 시 HTTP 메서드와 상태 코드를 어떤 기준으로 선택하는지 말해 주세요.' },
        { category: '기술', questionText: '데이터베이스 인덱스가 필요한 이유와 주의할 점을 설명해 주세요.' },
      ],
      MEDIUM: [
        { category: '문제해결', questionText: 'N+1 문제를 실제로 어떻게 탐지하고 해결할지 설명해 주세요.' },
        { category: '성능', questionText: '대량 트래픽 환경에서 API 성능 병목을 진단하는 절차를 설명해 주세요.' },
        { category: '설계', questionText: '예외 처리와 로깅 전략을 백엔드 서비스에서 어떻게 설계할지 말해 주세요.' },
      ],
      HARD: [
        { category: '설계', questionText: '모놀리식과 MSA 중 어떤 구조를 선택할지 판단 기준과 트레이드오프를 설명해 주세요.' },
        { category: '성능', questionText: '읽기 부하가 높은 시스템에서 데이터 일관성과 성능을 함께 관리하는 방식을 설명해 주세요.' },
      ],
    },
    FRONTEND: {
      EASY: [
        { category: '기술', questionText: 'React에서 state와 props의 차이와 각각의 역할을 설명해 주세요.' },
        { category: '기술', questionText: '브라우저 렌더링 과정과 reflow, repaint의 차이를 설명해 주세요.' },
        { category: '기술', questionText: 'CSR과 SSR의 차이와 각각이 유리한 상황을 설명해 주세요.' },
      ],
      MEDIUM: [
        { category: '문제해결', questionText: '불필요한 리렌더링을 찾고 줄이는 방법을 설명해 주세요.' },
        { category: '설계', questionText: '상태 관리 도구를 선택할 때 고려하는 기준과 트레이드오프를 설명해 주세요.' },
        { category: '성능', questionText: '프론트엔드 성능 저하가 발생했을 때 진단 순서와 측정 지표를 설명해 주세요.' },
      ],
      HARD: [
        { category: '설계', questionText: '복잡한 인터랙션이 많은 화면에서 렌더링 성능과 유지보수성을 함께 잡는 설계를 설명해 주세요.' },
        { category: '문제해결', questionText: '서버 상태와 클라이언트 상태가 복잡하게 얽힌 서비스에서 데이터 일관성을 어떻게 관리할지 말해 주세요.' },
      ],
    },
  },
  COMPREHENSIVE: {
    BACKEND: {
      EASY: [
        { category: '경험', questionText: '백엔드 개발자로서 본인의 강점 하나와 이를 보여준 프로젝트 경험을 함께 설명해 주세요.' },
        { category: '기술', questionText: '가장 익숙한 백엔드 기술 스택과 그 기술을 선택한 이유를 설명해 주세요.' },
        { category: '협업', questionText: '협업 과정에서 맡았던 백엔드 기능과 그 과정에서 배운 점을 말해 주세요.' },
      ],
      MEDIUM: [
        { category: '문제해결', questionText: '운영 중 발생한 백엔드 이슈를 해결한 경험과 팀 커뮤니케이션 방식을 함께 설명해 주세요.' },
        { category: '성능', questionText: '성능 개선 경험이 있다면 기술적 판단과 협업 과정을 함께 말해 주세요.' },
        { category: '설계', questionText: '백엔드 설계를 바꾸자고 제안했던 경험이 있다면 근거와 결과를 설명해 주세요.' },
      ],
      HARD: [
        { category: '설계', questionText: '대규모 트래픽 문제를 해결해야 하는 상황에서 기술적 대응과 팀 조율 방식을 함께 설명해 주세요.' },
        { category: '의사결정', questionText: '시스템 설계상 어려운 트레이드오프를 결정했던 경험이 있다면 조직 맥락까지 포함해 말해 주세요.' },
      ],
    },
    FRONTEND: {
      EASY: [
        { category: '경험', questionText: '프론트엔드 개발자로서 본인의 강점 하나와 이를 보여준 프로젝트 경험을 함께 설명해 주세요.' },
        { category: '기술', questionText: '가장 익숙한 프론트엔드 기술 스택과 그 기술을 선택한 이유를 설명해 주세요.' },
        { category: '협업', questionText: '협업 과정에서 맡았던 화면이나 기능과 그 과정에서 배운 점을 말해 주세요.' },
      ],
      MEDIUM: [
        { category: '문제해결', questionText: '프론트엔드 이슈를 해결한 경험과 디자인 또는 기획과의 협업 방식을 함께 설명해 주세요.' },
        { category: '성능', questionText: '렌더링 성능 개선 경험이 있다면 기술적 판단과 팀 커뮤니케이션을 함께 말해 주세요.' },
        { category: '설계', questionText: '컴포넌트 구조나 상태 관리를 개선하자고 제안했던 경험을 설명해 주세요.' },
      ],
      HARD: [
        { category: '설계', questionText: '대규모 화면 성능 문제를 해결할 때 기술적 대응과 팀 조율을 함께 설명해 주세요.' },
        { category: '의사결정', questionText: '복잡한 UI 요구사항과 일정 압박 사이에서 의사결정했던 경험을 설명해 주세요.' },
      ],
    },
  },
  RESUME_BASED: {
    BACKEND: {
      EASY: [
        { category: '지원서검증', questionText: '이력서나 지원서에 적은 백엔드 프로젝트 중 가장 자신 있는 경험 하나를 소개해 주세요.' },
        { category: '지원서검증', questionText: '지원서에 적은 기술 스택 중 실제로 가장 깊게 사용한 기술과 사용 맥락을 설명해 주세요.' },
        { category: '지원서검증', questionText: '작성한 경험 중 본인이 직접 기여한 부분과 팀의 기여를 구분해서 설명해 주세요.' },
      ],
      MEDIUM: [
        { category: '문제해결', questionText: '이력서에 적은 백엔드 성능 개선 경험에서 실제 병목 원인과 판단 근거를 설명해 주세요.' },
        { category: '지원서검증', questionText: '지원서에 적은 트러블슈팅 사례에서 문제 원인 분석 과정을 단계별로 설명해 주세요.' },
        { category: '설계', questionText: '프로젝트 설명에 적은 아키텍처 선택이 왜 적절했는지 당시 제약 조건까지 포함해 말해 주세요.' },
      ],
      HARD: [
        { category: '지원서검증', questionText: '이력서의 백엔드 성과 수치가 어떻게 측정된 것인지 지표와 검증 방법까지 설명해 주세요.' },
        { category: '의사결정', questionText: '지원서에 적은 핵심 역할이 본인 주도였음을 보여주는 의사결정 장면을 구체적으로 설명해 주세요.' },
      ],
    },
    FRONTEND: {
      EASY: [
        { category: '지원서검증', questionText: '이력서나 지원서에 적은 프론트엔드 프로젝트 중 가장 자신 있는 경험 하나를 소개해 주세요.' },
        { category: '지원서검증', questionText: '지원서에 적은 기술 스택 중 실제로 가장 깊게 사용한 프론트엔드 기술과 사용 맥락을 설명해 주세요.' },
        { category: '지원서검증', questionText: '작성한 경험 중 본인이 직접 기여한 화면이나 기능을 구체적으로 설명해 주세요.' },
      ],
      MEDIUM: [
        { category: '문제해결', questionText: '이력서에 적은 프론트엔드 성능 개선 경험에서 병목을 어떻게 확인했는지 설명해 주세요.' },
        { category: '지원서검증', questionText: '지원서에 적은 UI 개선 사례에서 사용자 문제를 어떻게 정의했고 무엇을 바꿨는지 설명해 주세요.' },
        { category: '설계', questionText: '프로젝트 설명에 적은 상태 관리나 구조 설계 선택이 왜 적절했는지 당시 제약과 함께 말해 주세요.' },
      ],
      HARD: [
        { category: '지원서검증', questionText: '이력서의 성과 수치가 실제 사용자 경험 개선으로 이어졌다는 근거를 어떻게 확인했는지 설명해 주세요.' },
        { category: '의사결정', questionText: '지원서에 적은 핵심 역할이 본인 주도였음을 보여주는 의사결정 장면을 구체적으로 설명해 주세요.' },
      ],
    },
  },
}

export const difficultyPlan = ['EASY', 'EASY', 'MEDIUM', 'MEDIUM', 'HARD']
