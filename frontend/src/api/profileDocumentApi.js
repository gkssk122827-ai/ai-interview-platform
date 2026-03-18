import apiClient, { extractApiErrorMessage } from './client.js'
import { cloneData, createId, delay, extractPayload } from './apiUtils.js'

const useStubApi = import.meta.env.VITE_USE_API_STUB === 'true'

const stubDocuments = [
  {
    id: 'profile-document-1',
    userId: 'user-demo',
    title: '백엔드 개발 지원자료',
    resumeText: 'Spring Boot, JPA, MariaDB 기반 백엔드 개발 경험을 정리한 이력서입니다.',
    coverLetterText: '문제 해결과 운영 자동화 경험을 중심으로 작성한 자기소개서입니다.',
    originalFileName: 'backend-profile.pdf',
    storedFilePath: 'stub/profile-document-1/backend-profile.pdf',
    fileUrl: '/api/v1/profile-documents/profile-document-1/file',
    createdAt: '2026-03-13T09:00:00',
    updatedAt: '2026-03-13T09:00:00',
  },
]

function nowIsoString() {
  return new Date().toISOString()
}

function normalizeStubDocument(payload, existing = null) {
  const file = payload.file ?? null
  return {
    ...existing,
    title: payload.title,
    resumeText: payload.resumeText || null,
    coverLetterText: payload.coverLetterText || null,
    originalFileName: file?.name ?? existing?.originalFileName ?? null,
    storedFilePath: file?.name != null ? `stub/${existing?.id ?? 'profile-document'}/${file.name}` : existing?.storedFilePath ?? null,
    fileUrl: file?.name != null ? `/api/v1/profile-documents/${existing?.id ?? 'profile-document'}/file` : existing?.fileUrl ?? null,
    updatedAt: nowIsoString(),
  }
}

function buildMultipartPayload(payload) {
  const formData = new FormData()
  formData.append('title', payload.title)
  if (payload.resumeText) formData.append('resumeText', payload.resumeText)
  if (payload.coverLetterText) formData.append('coverLetterText', payload.coverLetterText)
  if (payload.file instanceof File) formData.append('file', payload.file)
  return formData
}

const profileDocumentApi = {
  async list() {
    if (useStubApi) {
      await delay()
      return cloneData(stubDocuments)
    }

    try {
      const response = await apiClient.get('/profile-documents')
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '지원자료를 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async create(payload) {
    if (useStubApi) {
      await delay()
      const id = createId('profile-document')
      const createdAt = nowIsoString()
      const created = { id, userId: 'user-demo', createdAt, ...normalizeStubDocument(payload) }
      created.storedFilePath = payload.file ? `stub/${id}/${payload.file.name}` : null
      created.fileUrl = payload.file ? `/api/v1/profile-documents/${id}/file` : null
      stubDocuments.unshift(created)
      return cloneData(created)
    }

    try {
      const response = await apiClient.post('/profile-documents', buildMultipartPayload(payload), {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '지원자료를 저장하는 중 오류가 발생했습니다.'))
    }
  },

  async update(id, payload) {
    if (useStubApi) {
      await delay()
      const index = stubDocuments.findIndex((item) => item.id === id)
      if (index < 0) throw new Error('지원자료를 찾을 수 없습니다.')
      const updated = { ...stubDocuments[index], ...normalizeStubDocument(payload, stubDocuments[index]) }
      stubDocuments[index] = updated
      return cloneData(updated)
    }

    try {
      const hasFile = payload.file instanceof File
      const response = hasFile
        ? await apiClient.put(`/profile-documents/${id}`, buildMultipartPayload(payload), {
            headers: { 'Content-Type': 'multipart/form-data' },
          })
        : await apiClient.put(`/profile-documents/${id}`, {
            title: payload.title,
            resumeText: payload.resumeText || null,
            coverLetterText: payload.coverLetterText || null,
          })
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '지원자료를 수정하는 중 오류가 발생했습니다.'))
    }
  },

  async remove(id) {
    if (useStubApi) {
      await delay()
      const index = stubDocuments.findIndex((item) => item.id === id)
      if (index < 0) throw new Error('지원자료를 찾을 수 없습니다.')
      stubDocuments.splice(index, 1)
      return { success: true }
    }

    try {
      const response = await apiClient.delete(`/profile-documents/${id}`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '지원자료를 삭제하는 중 오류가 발생했습니다.'))
    }
  },
}

export default profileDocumentApi
