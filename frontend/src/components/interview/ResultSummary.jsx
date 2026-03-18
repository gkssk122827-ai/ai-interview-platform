function ResultSummary({ result }) {
  if (!result) return null

  const score = result.feedback?.overallScore ?? 0
  const items = result.questions ?? []
  const recommendations = result.learningRecommendations ?? []
  const highlights = result.highlights ?? []

  return (
    <section className="workspace-grid">
      <article className="panel">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">세션 요약</h3>
            <p className="panel__subtitle">전체 진행 현황과 완료 여부를 확인할 수 있습니다.</p>
          </div>
          <span className="score-chip">{result.completionRate ?? 0}%</span>
        </div>
        <p className="panel__subtitle"><strong>세션명</strong> {result.title || '-'}</p>
        <p className="panel__subtitle"><strong>지원 직무</strong> {result.positionTitle || '-'}</p>
        <p className="panel__subtitle"><strong>답변 완료</strong> {result.answeredQuestions ?? 0} / {result.totalQuestions ?? 0}</p>
        <p className="panel__subtitle"><strong>미응답</strong> {result.unansweredQuestions ?? 0}</p>
        <p className="panel__subtitle"><strong>진행 시간</strong> {result.durationMinutes ?? 0}분</p>
        <p className="panel__subtitle"><strong>상태</strong> {result.completed ? '면접 완료' : '진행 중'}</p>
      </article>

      <article className="panel">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">점수 분석</h3>
            <p className="panel__subtitle">저장된 답변을 기준으로 종합 점수를 요약했습니다.</p>
          </div>
          <span className="score-chip">{score}점</span>
        </div>
        <p className="panel__subtitle">관련성 {result.feedback?.relevanceScore ?? 0}점 · 논리성 {result.feedback?.logicScore ?? 0}점 · 구체성 {result.feedback?.specificityScore ?? 0}점</p>
        <p className="panel__subtitle">{result.summary ?? '요약 정보가 없습니다.'}</p>
      </article>

      <article className="panel panel--wide">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">종합 피드백</h3>
            <p className="panel__subtitle">다음 면접 전에 우선 확인하면 좋은 내용입니다.</p>
          </div>
        </div>
        <p className="panel__subtitle"><strong>약점</strong> {result.feedback?.weakPoints ?? '-'}</p>
        <p className="panel__subtitle"><strong>개선 방향</strong> {result.feedback?.improvements ?? '-'}</p>
        <p className="panel__subtitle"><strong>추천 답변 방식</strong> {result.feedback?.recommendedAnswer ?? '-'}</p>
        {highlights.length > 0 ? (
          <div className="resource-list">
            {highlights.map((item, index) => (
              <div key={`${item}-${index}`} className="resource-list__item resource-list__item--static">
                <strong>핵심 포인트 {index + 1}</strong>
                <span>{item}</span>
              </div>
            ))}
          </div>
        ) : null}
      </article>

      <article className="panel panel--wide">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">학습 추천</h3>
            <p className="panel__subtitle">이번 결과를 바탕으로 이어서 학습하면 좋은 항목입니다.</p>
          </div>
        </div>
        <div className="resource-list">
          {recommendations.length > 0 ? recommendations.map((item, index) => (
            <div key={`${item.focusArea}-${index}`} className="resource-list__item resource-list__item--static">
              <strong>{item.focusArea}</strong>
              <span>{item.reason}</span>
              <span>{item.recommendedAction}</span>
            </div>
          )) : <div className="resource-list__item resource-list__item--static"><strong>추천 학습 없음</strong><span>추가로 추천할 항목이 아직 없습니다.</span></div>}
        </div>
      </article>

      <article className="panel panel--wide result-summary-panel">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">질문별 답변 요약</h3>
            <p className="panel__subtitle">각 질문과 저장된 답변 내용을 가로 카드 형태로 한눈에 확인할 수 있습니다.</p>
          </div>
        </div>
        <div className="result-question-list">
          {items.map((item) => (
            <div key={item.id} className="result-question-item">
              <div className="result-question-item__header">
                <strong>{item.sequenceNumber}. {item.questionText}</strong>
              </div>
              <div className="interview-question-card__meta">
                <span className="dashboard-score-chip">{item.answered ? `답변 ${item.answerLength ?? 0}자` : '미응답'}</span>
              </div>
              <p className="result-question-item__question">{item.answerText || '아직 저장된 답변이 없습니다.'}</p>
            </div>
          ))}
        </div>
      </article>
    </section>
  )
}

export default ResultSummary