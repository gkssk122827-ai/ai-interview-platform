import apiClient, { extractApiErrorMessage } from './client.js'
import { createVoiceError } from '../constants/voiceMessages.js'

function extractPayload(response) {
  return response?.data?.data ?? response?.data ?? null
}

const voiceApi = {
  async speechToText(audioBlob, languageCode = 'ko-KR') {
    const formData = new FormData()
    formData.append('audio', audioBlob, 'recording.webm')
    if (languageCode) {
      formData.append('languageCode', languageCode)
    }

    try {
      const response = await apiClient.post('/stt', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      return extractPayload(response)
    } catch (error) {
      const responseCode = error?.response?.data?.error?.code
      if (!error?.response) {
        throw createVoiceError('VOICE_STT_NETWORK_ERROR')
      }
      if (responseCode) {
        throw createVoiceError(responseCode)
      }
      if (error?.response?.status >= 500) {
        throw createVoiceError('VOICE_STT_SERVER_ERROR')
      }
      throw createVoiceError('', extractApiErrorMessage(error, '음성 인식에 실패했습니다.'))
    }
  },

  async textToSpeech({ text, voiceName = '', languageCode = 'ko-KR' }) {
    try {
      const response = await apiClient.post('/tts', {
        text,
        voiceName,
        languageCode,
      })
      return extractPayload(response)
    } catch (error) {
      const responseCode = error?.response?.data?.error?.code
      if (!error?.response) {
        throw createVoiceError('VOICE_TTS_NETWORK_ERROR')
      }
      if (responseCode) {
        throw createVoiceError(responseCode)
      }
      if (error?.response?.status >= 500) {
        throw createVoiceError('VOICE_TTS_SERVER_ERROR')
      }
      throw createVoiceError('', extractApiErrorMessage(error, '음성 생성에 실패했습니다.'))
    }
  },
}

export default voiceApi

