UPDATE interview_feedback
SET weak_points = '아직 저장된 답변이 없어 결과를 분석할 수 없습니다.',
    improvements = '각 답변을 두세 문장 이상으로 작성하고, 본인의 역할과 선택 이유를 함께 설명해 보세요.',
    recommended_answer = '상황, 행동, 결과를 순서대로 정리하고 성과 수치나 근거를 덧붙이면 더 좋은 답변이 됩니다.'
WHERE overall_score = 0
  AND relevance_score = 0
  AND logic_score = 0
  AND specificity_score = 0
  AND (
    weak_points NOT REGEXP '[가-힣]'
    OR improvements NOT REGEXP '[가-힣]'
    OR recommended_answer NOT REGEXP '[가-힣]'
  );

UPDATE interview_feedback
SET weak_points = '답변 길이와 근거를 조금 더 보강하면 설득력이 좋아집니다.'
WHERE weak_points IS NOT NULL
  AND weak_points <> ''
  AND weak_points NOT REGEXP '[가-힣]';

UPDATE interview_feedback
SET improvements = '상황, 행동, 결과를 순서대로 정리하고 성과 수치나 사용 기술을 함께 설명해 보세요.'
WHERE improvements IS NOT NULL
  AND improvements <> ''
  AND improvements NOT REGEXP '[가-힣]';

UPDATE interview_feedback
SET recommended_answer = '질문 의도를 먼저 짚고 본인의 역할, 해결 과정, 결과, 배운 점 순서로 답변해 보세요.'
WHERE recommended_answer IS NOT NULL
  AND recommended_answer <> ''
  AND recommended_answer NOT REGEXP '[가-힣]';
