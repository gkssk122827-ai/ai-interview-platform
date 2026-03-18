function FeedbackCard({
  feedback,
  title = 'AI 피드백',
  description = '답변을 저장하면 현재 상태에 대한 피드백을 보여 줍니다.',
}) {
  return (
    <article className="panel">
      <div className="panel__header">
        <div>
          <h3 className="panel__title">{title}</h3>
          <p className="panel__subtitle">{description}</p>
        </div>
      </div>
      <p className="panel__subtitle">{feedback || '아직 생성된 피드백이 없습니다.'}</p>
    </article>
  )
}

export default FeedbackCard