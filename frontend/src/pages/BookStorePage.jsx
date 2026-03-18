import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import bookApi from '../api/bookApi.js'
import cartApi from '../api/cartApi.js'
import { BUTTON_LABELS, EMPTY_MESSAGES, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'

function BookStorePage() {
  usePageTitle('도서')
  const navigate = useNavigate()
  const [books, setBooks] = useState([])
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 })
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [statusMessage, setStatusMessage] = useState('')

  useEffect(() => {
    async function loadBooks() {
      setIsLoading(true)
      setError('')
      try {
        const result = await bookApi.list({ keyword, page, size: 8 })
        setBooks(result.content ?? [])
        setPageInfo({ totalPages: result.totalPages ?? 0, totalElements: result.totalElements ?? 0 })
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadBooks()
  }, [keyword, page])

  async function handleAddToCart(bookId) {
    try {
      await cartApi.addItem(bookId, 1)
      setStatusMessage('장바구니에 담았습니다.')
    } catch (addError) {
      setError(addError.message)
    }
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">도서 스토어</p>
        <h2 className="page-card__title">면접과 학습에 필요한 도서를 찾아보세요.</h2>
        <p className="page-card__description">검색과 페이지 이동, 장바구니 담기까지 실제 API와 연결됩니다.</p>
      </div>

      <StatusMessage variant="success" message={statusMessage} />
      <StatusMessage variant="error" message={error} />

      <section className="panel">
        <TextInput label="도서 검색" value={keyword} onChange={(event) => { setPage(0); setKeyword(event.target.value) }} placeholder="제목, 저자, 출판사로 검색해 보세요." />
      </section>

      {isLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingBooks} /> : null}
      {!isLoading && error ? <ErrorBlock message={error} /> : null}
      {!isLoading && !error && books.length === 0 ? <EmptyState title={EMPTY_MESSAGES.books.title} description={EMPTY_MESSAGES.books.description} /> : null}

      {!isLoading && !error && books.length > 0 ? <section className="workspace-grid">{books.map((book) => <article key={book.id} className="panel"><div className="panel__header"><div><h3 className="panel__title">{book.title}</h3><p className="panel__subtitle">{book.author} · {book.publisher}</p></div></div><p className="panel__subtitle">가격: {book.price}원</p><p className="panel__subtitle">재고: {book.stock}</p><p className="panel__subtitle">{book.description || '도서 설명이 없습니다.'}</p><div className="button-row"><button className="button" type="button" onClick={() => handleAddToCart(book.id)}>{BUTTON_LABELS.addToCart}</button></div></article>)}</section> : null}

      <div className="button-row">
        <button className="button button--secondary" type="button" disabled={page <= 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>이전</button>
        <span className="panel__subtitle">{page + 1} / {Math.max(pageInfo.totalPages, 1)} 페이지 · 총 {pageInfo.totalElements}권</span>
        <button className="button button--secondary" type="button" disabled={page + 1 >= pageInfo.totalPages} onClick={() => setPage((current) => current + 1)}>다음</button>
        <button className="button" type="button" onClick={() => navigate('/cart')}>장바구니 보기</button>
      </div>
    </section>
  )
}

export default BookStorePage
