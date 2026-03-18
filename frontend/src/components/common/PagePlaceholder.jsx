import { PLACEHOLDER_TEXT } from '../../constants/messages.js'

function PagePlaceholder({ title, description, bullets = [], highlights = [] }) {
  const items = bullets.length > 0 ? bullets : highlights

  return (
    <section className="page-card">
      <div className="page-card__content">
        <p className="page-card__eyebrow">{PLACEHOLDER_TEXT.eyebrow}</p>
        <h2 className="page-card__title">{title}</h2>
        <p className="page-card__description">{description}</p>
      </div>

      <div className="page-card__panel">
        <h3 className="page-card__panel-title">{PLACEHOLDER_TEXT.scopeTitle}</h3>
        <ul className="page-card__list">
          {items.map((item) => <li key={item}>{item}</li>)}
        </ul>
      </div>
    </section>
  )
}

export default PagePlaceholder
