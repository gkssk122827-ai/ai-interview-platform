import { useEffect } from 'react'

function usePageTitle(title) {
  useEffect(() => {
    document.title = `${title} | AI 면접 플랫폼`
  }, [title])
}

export default usePageTitle
