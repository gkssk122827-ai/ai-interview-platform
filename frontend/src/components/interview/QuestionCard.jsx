function QuestionCard({ question, index, total, mode }) {
  const answeredLabel = question?.answered ? '답변 작성 완료' : '아직 답변 전'

  return (
    <article className="panel interview-card">
      <div className="panel__header">
        <div>
          <p className="panel__subtitle">질문 {question?.sequenceNumber ?? index + 1} / {total}</p>
          <h3 className="panel__title">{question?.questionText}</h3>
          <p className="panel__subtitle">{answeredLabel}</p>
        </div>
        <span className="score-chip">{mode}</span>
      </div>
    </article>
  )
}

export default QuestionCard
