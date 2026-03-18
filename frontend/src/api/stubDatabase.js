import { cloneData, createId, delay } from './apiUtils.js'

const database = {
  users: [
    {
      id: 'user-demo',
      name: '데모 사용자',
      email: 'demo@aimentor.dev',
      password: 'password123',
    },
  ],
  resumes: [
    {
      id: 'resume-1',
      title: '백엔드 개발자 이력서',
      content: '플랫폼 엔지니어링 직무 지원을 위한 Spring Boot, MySQL 중심 이력서입니다.',
      updatedAt: '2026-03-13',
    },
  ],
  coverLetters: [
    {
      id: 'cover-letter-1',
      title: '카카오뱅크 지원 초안',
      companyName: '카카오뱅크',
      position: '백엔드 엔지니어',
      content: '안정적인 금융 서비스를 만들고, 깔끔한 API와 높은 책임감으로 제품 품질을 높이는 일을 좋아합니다.',
      updatedAt: '2026-03-13',
    },
  ],
  jobPostings: [
    {
      id: 'job-posting-1',
      companyName: '카카오뱅크',
      positionTitle: '플랫폼 백엔드 엔지니어',
      description: '안정적인 플랫폼 서비스를 구축하고 운영하며, 개발 생산성과 운영 품질을 함께 책임집니다.',
      jobUrl: 'https://example.com/jobs/platform-backend',
      deadline: '2026-03-31',
      updatedAt: '2026-03-13',
    },
  ],
}

function today() {
  return new Date().toISOString().slice(0, 10)
}

export async function stubLogin({ email, password }) {
  await delay()
  const user = database.users.find(
    (candidate) => candidate.email === email && candidate.password === password,
  )

  if (!user) {
    throw new Error('이메일 또는 비밀번호가 올바르지 않습니다.')
  }

  return {
    user: {
      id: user.id,
      name: user.name,
      email: user.email,
    },
    accessToken: 'stub-access-token',
    refreshToken: 'stub-refresh-token',
  }
}

export async function stubSignup(payload) {
  await delay()
  const exists = database.users.some((user) => user.email === payload.email)

  if (exists) {
    throw new Error('이미 사용 중인 이메일입니다.')
  }

  const newUser = {
    id: createId('user'),
    name: payload.name,
    email: payload.email,
    password: payload.password,
  }

  database.users.unshift(newUser)

  return {
    user: {
      id: newUser.id,
      name: newUser.name,
      email: newUser.email,
    },
    accessToken: 'stub-access-token',
    refreshToken: 'stub-refresh-token',
  }
}

export function createStubCrud(collectionName, prefix) {
  return {
    async list() {
      await delay()
      return cloneData(database[collectionName])
    },
    async create(payload) {
      await delay()
      const item = {
        ...payload,
        id: createId(prefix),
        updatedAt: today(),
      }
      database[collectionName].unshift(item)
      return cloneData(item)
    },
    async update(id, payload) {
      await delay()
      const index = database[collectionName].findIndex((item) => item.id === id)

      if (index < 0) {
        throw new Error('항목을 찾을 수 없습니다.')
      }

      const updated = {
        ...database[collectionName][index],
        ...payload,
        id,
        updatedAt: today(),
      }
      database[collectionName][index] = updated
      return cloneData(updated)
    },
    async remove(id) {
      await delay()
      const index = database[collectionName].findIndex((item) => item.id === id)

      if (index < 0) {
        throw new Error('항목을 찾을 수 없습니다.')
      }

      database[collectionName].splice(index, 1)
      return { success: true }
    },
  }
}
