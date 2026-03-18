INSERT INTO users (
    email,
    name,
    phone,
    password,
    role,
    refresh_token,
    refresh_token_expires_at,
    created_at,
    updated_at
)
SELECT
    'seed.user@aimentor.local',
    '샘플 사용자',
    '010-1111-2222',
    '$2a$10$lfICPEALvbPQ5K6o7O4JR.p9/N3fRa9KGTNcYI0F00eOMGGRqM9km',
    'USER',
    NULL,
    NULL,
    NOW(6),
    NOW(6)
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'seed.user@aimentor.local'
);

INSERT INTO users (
    email,
    name,
    phone,
    password,
    role,
    refresh_token,
    refresh_token_expires_at,
    created_at,
    updated_at
)
SELECT
    'seed.admin@aimentor.local',
    '샘플 관리자',
    '010-9999-0000',
    '$2a$10$lfICPEALvbPQ5K6o7O4JR.p9/N3fRa9KGTNcYI0F00eOMGGRqM9km',
    'ADMIN',
    NULL,
    NULL,
    NOW(6),
    NOW(6)
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'seed.admin@aimentor.local'
);

INSERT INTO application_documents (
    user_id,
    title,
    resume_text,
    cover_letter_text,
    original_file_name,
    stored_file_path,
    file_url,
    created_at,
    updated_at
)
SELECT
    u.id,
    '백엔드 개발자 지원자료',
    'Spring Boot와 JPA 기반의 REST API 설계 및 운영 경험이 있습니다. 장애 대응 자동화와 성능 개선 프로젝트를 수행했습니다.',
    '문제 해결 과정을 구조적으로 설명하고, 운영 환경에서 검증된 개선 경험으로 팀에 기여하고 싶습니다.',
    NULL,
    NULL,
    NULL,
    NOW(6),
    NOW(6)
FROM users u
WHERE u.email = 'seed.user@aimentor.local'
  AND NOT EXISTS (
      SELECT 1
      FROM application_documents ad
      WHERE ad.user_id = u.id
        AND ad.title = '백엔드 개발자 지원자료'
  );

INSERT INTO application_documents (
    user_id,
    title,
    resume_text,
    cover_letter_text,
    original_file_name,
    stored_file_path,
    file_url,
    created_at,
    updated_at
)
SELECT
    u.id,
    '플랫폼 서버 지원자료',
    '분산 시스템 환경에서 인증, 파일 업로드, 로그 추적 체계를 구축한 경험이 있습니다. MariaDB와 Redis 운영 경험도 보유하고 있습니다.',
    '사용자 경험과 운영 안정성을 함께 고려하는 개발자로서, AI 모의면접 플랫폼의 신뢰성을 높이는 데 집중하겠습니다.',
    NULL,
    NULL,
    NULL,
    NOW(6),
    NOW(6)
FROM users u
WHERE u.email = 'seed.user@aimentor.local'
  AND NOT EXISTS (
      SELECT 1
      FROM application_documents ad
      WHERE ad.user_id = u.id
        AND ad.title = '플랫폼 서버 지원자료'
  );

INSERT INTO application_documents (
    user_id,
    title,
    resume_text,
    cover_letter_text,
    original_file_name,
    stored_file_path,
    file_url,
    created_at,
    updated_at
)
SELECT
    u.id,
    'AI 서비스 연동 지원자료',
    'FastAPI 기반 AI 서버와 Spring Boot 백엔드를 연동하며, 인터뷰 질문 생성과 학습 문제 채점 API를 설계했습니다.',
    'AI 기능을 사용자 흐름에 자연스럽게 녹이는 제품 개발 경험을 바탕으로, 서비스 품질을 높이겠습니다.',
    NULL,
    NULL,
    NULL,
    NOW(6),
    NOW(6)
FROM users u
WHERE u.email = 'seed.user@aimentor.local'
  AND NOT EXISTS (
      SELECT 1
      FROM application_documents ad
      WHERE ad.user_id = u.id
        AND ad.title = 'AI 서비스 연동 지원자료'
  );

INSERT INTO job_postings (
    user_id,
    company_name,
    position_title,
    description,
    file_url,
    job_url,
    deadline,
    created_at,
    updated_at
)
SELECT
    u.id,
    '에이아이멘토',
    '백엔드 개발자',
    'Spring Boot, JPA, MariaDB 기반 서비스 개발 경험이 있는 백엔드 개발자를 찾습니다. 인증, 파일 처리, 운영 로그 분석 경험을 우대합니다.',
    NULL,
    'https://example.com/jobs/backend-engineer',
    DATE_ADD(CURDATE(), INTERVAL 21 DAY),
    NOW(6),
    NOW(6)
FROM users u
WHERE u.email = 'seed.admin@aimentor.local'
  AND NOT EXISTS (
      SELECT 1
      FROM job_postings jp
      WHERE jp.company_name = '에이아이멘토'
        AND jp.position_title = '백엔드 개발자'
  );

INSERT INTO job_postings (
    user_id,
    company_name,
    position_title,
    description,
    file_url,
    job_url,
    deadline,
    created_at,
    updated_at
)
SELECT
    u.id,
    '브라이트에듀',
    '프론트엔드 개발자',
    'React, Vite, 상태관리 라이브러리 사용 경험이 있는 프론트엔드 개발자를 채용합니다. 교육 플랫폼 UI/UX 개선 경험을 우대합니다.',
    NULL,
    'https://example.com/jobs/frontend-engineer',
    DATE_ADD(CURDATE(), INTERVAL 18 DAY),
    NOW(6),
    NOW(6)
FROM users u
WHERE u.email = 'seed.admin@aimentor.local'
  AND NOT EXISTS (
      SELECT 1
      FROM job_postings jp
      WHERE jp.company_name = '브라이트에듀'
        AND jp.position_title = '프론트엔드 개발자'
  );

INSERT INTO job_postings (
    user_id,
    company_name,
    position_title,
    description,
    file_url,
    job_url,
    deadline,
    created_at,
    updated_at
)
SELECT
    u.id,
    '데이터웨이브',
    'AI 서비스 엔지니어',
    'FastAPI, LLM API 연동, 비동기 처리 경험이 있는 엔지니어를 찾습니다. 프롬프트 설계와 응답 품질 개선 경험이 있으면 좋습니다.',
    NULL,
    'https://example.com/jobs/ai-service-engineer',
    DATE_ADD(CURDATE(), INTERVAL 25 DAY),
    NOW(6),
    NOW(6)
FROM users u
WHERE u.email = 'seed.admin@aimentor.local'
  AND NOT EXISTS (
      SELECT 1
      FROM job_postings jp
      WHERE jp.company_name = '데이터웨이브'
        AND jp.position_title = 'AI 서비스 엔지니어'
  );

INSERT INTO job_postings (
    user_id,
    company_name,
    position_title,
    description,
    file_url,
    job_url,
    deadline,
    created_at,
    updated_at
)
SELECT
    u.id,
    '클라우드패스',
    'DevOps 엔지니어',
    'AWS, Docker, GitHub Actions 기반 배포 자동화 경험을 가진 DevOps 엔지니어를 채용합니다. 모니터링 및 보안 설정 경험을 우대합니다.',
    NULL,
    'https://example.com/jobs/devops-engineer',
    DATE_ADD(CURDATE(), INTERVAL 30 DAY),
    NOW(6),
    NOW(6)
FROM users u
WHERE u.email = 'seed.admin@aimentor.local'
  AND NOT EXISTS (
      SELECT 1
      FROM job_postings jp
      WHERE jp.company_name = '클라우드패스'
        AND jp.position_title = 'DevOps 엔지니어'
  );

INSERT INTO job_postings (
    user_id,
    company_name,
    position_title,
    description,
    file_url,
    job_url,
    deadline,
    created_at,
    updated_at
)
SELECT
    u.id,
    '러닝포지',
    '교육 플랫폼 PM',
    '교육 서비스 기획, 사용자 인터뷰, 데이터 기반 개선 경험을 가진 PM을 찾습니다. AI 학습 기능 운영 경험이 있으면 좋습니다.',
    NULL,
    'https://example.com/jobs/product-manager',
    DATE_ADD(CURDATE(), INTERVAL 14 DAY),
    NOW(6),
    NOW(6)
FROM users u
WHERE u.email = 'seed.admin@aimentor.local'
  AND NOT EXISTS (
      SELECT 1
      FROM job_postings jp
      WHERE jp.company_name = '러닝포지'
        AND jp.position_title = '교육 플랫폼 PM'
  );
