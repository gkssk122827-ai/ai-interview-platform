import { useEffect, useState } from 'react'
import { ERROR_MESSAGES } from '../constants/messages.js'

function useCrudResource(api) {
  const [items, setItems] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  useEffect(() => {
    let active = true

    async function loadItems() {
      setIsLoading(true)
      setError('')

      try {
        const loadedItems = await api.list()
        if (!active) return
        setItems(Array.isArray(loadedItems) ? loadedItems : [])
        setSelectedId((current) => current ?? loadedItems?.[0]?.id ?? null)
      } catch (loadError) {
        if (!active) return
        setError(loadError.message ?? ERROR_MESSAGES.loadList)
      } finally {
        if (active) setIsLoading(false)
      }
    }

    loadItems()
    return () => {
      active = false
    }
  }, [api])

  async function saveItem(payload) {
    setIsSaving(true)
    setError('')
    setSuccessMessage('')

    try {
      if (selectedId) {
        const updated = await api.update(selectedId, payload)
        setItems((current) => current.map((item) => (item.id === selectedId ? updated : item)))
        setSuccessMessage('수정되었습니다.')
        return updated
      }

      const created = await api.create(payload)
      setItems((current) => [created, ...current])
      setSelectedId(created.id)
      setSuccessMessage('저장되었습니다.')
      return created
    } catch (saveError) {
      setError(saveError.message ?? ERROR_MESSAGES.saveItem)
      throw saveError
    } finally {
      setIsSaving(false)
    }
  }

  async function deleteItem(id) {
    setError('')
    setSuccessMessage('')

    try {
      await api.remove(id)
      setItems((current) => current.filter((item) => item.id !== id))
      setSelectedId((current) => (current === id ? null : current))
      setSuccessMessage('삭제되었습니다.')
    } catch (deleteError) {
      setError(deleteError.message ?? ERROR_MESSAGES.deleteItem)
      throw deleteError
    }
  }

  return { items, selectedId, setSelectedId, isLoading, isSaving, error, setError, successMessage, setSuccessMessage, saveItem, deleteItem }
}

export default useCrudResource
