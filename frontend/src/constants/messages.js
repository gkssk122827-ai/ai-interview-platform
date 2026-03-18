export const BUTTON_LABELS = {
  save: '저장',
  delete: '삭제',
  edit: '수정',
  create: '생성',
  reset: '초기화',
  login: '로그인',
  register: '회원가입',
  logout: '로그아웃',
  newItem: '새로 만들기',
  newDocument: '새 지원자료',
  startInterview: '면접 시작',
  startLearning: '학습 시작',
  goToSetup: '설정 화면으로 이동',
  goToDashboard: '대시보드로 이동',
  prepareRecording: '녹음 준비',
  submitAnswer: '답변 저장',
  nextQuestion: '다음 질문',
  viewResult: '결과 보기',
  addToCart: '장바구니 담기',
  orderNow: '바로 주문',
}

export const STATUS_MESSAGES = {
  loadingData: '데이터를 불러오는 중입니다.',
  loadingList: '목록을 불러오는 중입니다.',
  loadingDocuments: '지원자료를 불러오는 중입니다.',
  loadingInterviewSetup: '면접 설정 정보를 불러오는 중입니다.',
  generatingInterviewQuestions: 'AI 면접 질문을 준비하는 중입니다.',
  loadingInterviewSession: '면접 세션을 불러오는 중입니다.',
  loadingInterviewResult: '면접 결과를 불러오는 중입니다.',
  loadingLearningResult: '채점 결과를 불러오는 중입니다.',
  loadingBooks: '도서를 불러오는 중입니다.',
  loadingCart: '장바구니를 불러오는 중입니다.',
  loadingOrders: '주문 내역을 불러오는 중입니다.',
  loadingDashboard: '대시보드를 불러오는 중입니다.',
  loadingAdminDashboard: '관리자 대시보드를 불러오는 중입니다.',
  signingIn: '로그인하는 중입니다.',
  creatingAccount: '회원가입하는 중입니다.',
  saving: '저장하는 중입니다.',
  preparing: '준비하는 중입니다.',
}

export const ERROR_MESSAGES = {
  generic: '오류가 발생했습니다.',
  retry: '입력 내용을 다시 확인해 주세요.',
  network: '서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.',
  badRequest: '요청 내용을 다시 확인해 주세요.',
  unauthorized: '로그인이 필요합니다.',
  forbidden: '이 기능을 사용할 권한이 없습니다.',
  notFound: '요청한 정보를 찾을 수 없습니다.',
  server: '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
  sessionExpired: '세션이 만료되었습니다. 다시 로그인해 주세요.',
  missingRefreshToken: '로그인 정보를 다시 확인해 주세요.',
  loadList: '목록을 불러오는 중 오류가 발생했습니다.',
  saveItem: '저장하는 중 오류가 발생했습니다.',
  deleteItem: '삭제하는 중 오류가 발생했습니다.',
  login: '로그인 중 오류가 발생했습니다.',
  signup: '회원가입 중 오류가 발생했습니다.',
}

export const PLACEHOLDER_TEXT = {
  eyebrow: '준비 중',
  scopeTitle: '현재 진행 범위',
}

export const NAV_TEXT = {
  login: '로그인',
  register: '회원가입',
  dashboard: '대시보드',
  documents: '지원자료',
  jobPosting: '채용공고',
  interview: '모의면접',
  learning: '학습',
  books: '도서',
  cart: '장바구니',
  orders: '주문',
  admin: '관리자',
  aria: '주요 메뉴',
}

export const COMMON_TEXT = {
  list: '목록',
  total: '전체',
  errorTitle: '오류',
  emptyTitle: '표시할 내용이 없습니다.',
  sessionNoticeTitle: '안내',
  noFile: '첨부된 파일이 없습니다.',
  attachedFile: '첨부 파일',
  savedFile: '저장된 파일',
}

export const EMPTY_MESSAGES = {
  documents: {
    title: '등록된 지원자료가 없습니다.',
    description: '지원자료를 저장하면 면접 설정 화면에서 바로 선택할 수 있습니다.',
  },
  interviewDocuments: {
    title: '선택할 지원자료가 없습니다.',
    description: '먼저 지원자료를 등록한 뒤 모의면접을 시작해 주세요.',
  },
  interviewJobPostings: {
    title: '등록된 채용공고가 없습니다.',
    description: '채용공고 없이도 면접은 시작할 수 있지만, 공고를 선택하면 더 구체적인 질문으로 연습할 수 있습니다.',
  },
  books: {
    title: '표시할 도서가 없습니다.',
    description: '검색 조건을 바꾸거나 잠시 후 다시 시도해 주세요.',
  },
  cart: {
    title: '장바구니가 비어 있습니다.',
    description: '도서 목록에서 필요한 도서를 담아 보세요.',
  },
  orders: {
    title: '주문 내역이 없습니다.',
    description: '도서를 주문하면 여기에서 상태와 상세 내역을 확인할 수 있습니다.',
  },
  learningProblems: {
    title: '생성된 학습 문제가 없습니다.',
    description: '학습 설정으로 돌아가 과목과 난이도를 다시 선택해 주세요.',
  },
  adminDailySignups: {
    title: '표시할 가입 통계가 없습니다.',
    description: '회원 데이터가 쌓이면 최근 가입 추이를 차트로 보여드립니다.',
  },
  adminRecentUsers: {
    title: '최근 가입한 사용자가 없습니다.',
    description: '사용자 데이터가 쌓이면 이 영역에서 최근 가입 사용자를 확인할 수 있습니다.',
  },
  adminRecentDocuments: {
    title: '최근 지원자료가 없습니다.',
    description: '사용자가 지원자료를 등록하면 이 영역에 최근 항목이 표시됩니다.',
  },
  adminRecentSessions: {
    title: '최근 면접 세션이 없습니다.',
    description: '모의면접을 시작하면 이 영역에 최근 세션이 표시됩니다.',
  },
}