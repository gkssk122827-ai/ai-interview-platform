import { create } from 'zustand'

const useAppStore = create(() => ({
  recentInterviewSessions: [
    { id: 'session-1', companyName: 'AIMentor', date: '2026-03-10', score: 65 },
    { id: 'session-2', companyName: 'AIMentor', date: '2026-03-13', score: 72 },
    { id: 'session-3', companyName: 'AIMentor', date: '2026-03-16', score: 80 },
  ],
  scoreTrend: [
    { date: '3/10', score: 65 },
    { date: '3/13', score: 72 },
    { date: '3/16', score: 80 },
  ],
  weaknessTags: ['구체성', '답변 구조', '기술 설명', 'API 설계'],
  recommendedNextActions: [
    '자기소개와 문제 해결 경험을 STAR 구조로 다시 정리해 보세요.',
    '최근 프로젝트에서 API를 어떻게 설계했는지 근거를 포함해 연습해 보세요.',
    '직무 적합성을 보여 줄 사례를 한 가지 더 준비해 보세요.',
  ],
}))

export default useAppStore
