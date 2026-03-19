export const VOICE_ERROR_MESSAGES = {
  VOICE_STT_MIC_PERMISSION_DENIED: '마이크 권한이 필요합니다.',
  VOICE_STT_RECORDING_EMPTY: '녹음된 음성이 없습니다.',
  VOICE_STT_EMPTY_AUDIO: '녹음 파일이 비어 있습니다.',
  VOICE_STT_EMPTY_TRANSCRIPT: '음성 인식 결과가 없습니다.',
  VOICE_STT_PROVIDER_FAILED: '음성 인식에 실패했습니다.',
  VOICE_STT_NETWORK_ERROR: '네트워크 상태를 확인해 주세요.',
  VOICE_STT_SERVER_ERROR: '음성 인식 서버 오류가 발생했습니다.',
  VOICE_TTS_EMPTY_TEXT: '읽을 질문 텍스트가 없습니다.',
  VOICE_TTS_PROVIDER_FAILED: '음성 생성에 실패했습니다.',
  VOICE_TTS_INVALID_RESPONSE: '음성 생성 응답이 올바르지 않습니다.',
  VOICE_TTS_AUDIO_URL_MISSING: '재생 가능한 음성 데이터가 없습니다.',
  VOICE_TTS_PLAYBACK_FAILED: '음성 재생에 실패했습니다.',
  VOICE_TTS_AUTOPLAY_BLOCKED: '브라우저 자동 재생 제한으로 음성을 재생하지 못했습니다.',
  VOICE_TTS_NETWORK_ERROR: '네트워크 상태를 확인해 주세요.',
  VOICE_TTS_SERVER_ERROR: '음성 생성 서버 오류가 발생했습니다.',
}

export function getVoiceErrorMessage(code, fallbackMessage = '음성 처리 중 오류가 발생했습니다.') {
  if (!code) return fallbackMessage
  return VOICE_ERROR_MESSAGES[code] ?? fallbackMessage
}

export function createVoiceError(code, fallbackMessage) {
  const error = new Error(getVoiceErrorMessage(code, fallbackMessage))
  error.code = code
  return error
}

