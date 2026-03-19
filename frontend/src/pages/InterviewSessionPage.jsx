import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import AnswerInput from '../components/interview/AnswerInput.jsx'
import FeedbackCard from '../components/interview/FeedbackCard.jsx'
import QuestionCard from '../components/interview/QuestionCard.jsx'
import interviewApi from '../api/interviewApi.js'
import voiceApi from '../api/voiceApi.js'
import { BUTTON_LABELS, STATUS_MESSAGES } from '../constants/messages.js'
import { createVoiceError, getVoiceErrorMessage } from '../constants/voiceMessages.js'
import usePageTitle from '../hooks/usePageTitle.js'

const modeLabels = {
  COMPREHENSIVE: '\uC885\uD569',
  BEHAVIORAL: '\uC778\uC131',
  TECHNICAL: '\uAE30\uC220',
  RESUME_BASED: '\uC790\uC18C\uC11C \uAE30\uBC18',
}

function InterviewSessionPage() {
  usePageTitle('\uBA74\uC811 \uC9C4\uD589')

  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const sessionId = searchParams.get('sessionId')
  const [session, setSession] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [pageError, setPageError] = useState('')
  const [sessionSource, setSessionSource] = useState(location.state?.sessionSource ?? null)
  const [statusMessage, setStatusMessage] = useState(location.state?.sessionNotice ?? '')
  const [statusVariant, setStatusVariant] = useState(location.state?.sessionFallbackUsed ? 'error' : 'success')
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0)
  const [answer, setAnswer] = useState('')
  const [isRecording, setIsRecording] = useState(false)
  const [isVoiceLoading, setIsVoiceLoading] = useState(false)
  const [voiceError, setVoiceError] = useState('')
  const [isAutoPlayEnabled, setIsAutoPlayEnabled] = useState(true)
  const mediaRecorderRef = useRef(null)
  const mediaStreamRef = useRef(null)
  const recordedChunksRef = useRef([])
  const audioRef = useRef(null)

  useEffect(() => {
    async function loadSession() {
      if (!sessionId) {
        setPageError('\uBA74\uC811 \uC138\uC158 \uC815\uBCF4\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.')
        setIsLoading(false)
        return
      }

      setIsLoading(true)
      setPageError('')

      try {
        const loadedSession = await interviewApi.getSession(sessionId)
        setSession(loadedSession)
        setSessionSource((currentValue) => currentValue ?? loadedSession.questionGenerationSource ?? null)
        const nextIndex = loadedSession.questions.findIndex((question) => !question.answered)
        const initialIndex = nextIndex >= 0 ? nextIndex : 0
        setCurrentQuestionIndex(initialIndex)
        setAnswer(loadedSession.questions[initialIndex]?.answerText ?? '')
      } catch (error) {
        setPageError(error.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadSession()
  }, [sessionId])

  const currentQuestion = useMemo(() => session?.questions?.[currentQuestionIndex] ?? null, [session, currentQuestionIndex])
  const feedbackMessage = session?.feedback?.improvements ?? session?.feedback?.weakPoints ?? '\uB2F5\uBCC0\uC744 \uC81C\uCD9C\uD558\uBA74 \uB2E4\uC74C \uAC1C\uC120 \uBC29\uD5A5\uC744 \uC548\uB0B4\uD574\uB4DC\uB9BD\uB2C8\uB2E4.'
  const isLastQuestion = session ? currentQuestionIndex >= session.questions.length - 1 : false
  const normalizedAnswer = answer.trim()
  const existingAnswer = currentQuestion?.answerText?.trim() ?? ''
  const canAdvance = Boolean(normalizedAnswer) || Boolean(existingAnswer)
  const modeLabel = modeLabels[session?.mode] ?? '\uBA74\uC811'

  useEffect(() => {
    return () => {
      if (window.speechSynthesis) {
        window.speechSynthesis.cancel()
      }
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current = null
      }
      if (mediaStreamRef.current) {
        mediaStreamRef.current.getTracks().forEach((track) => track.stop())
        mediaStreamRef.current = null
      }
    }
  }, [])

  useEffect(() => {
    let cancelled = false

    async function playQuestionVoice() {
      if (!isAutoPlayEnabled || !currentQuestion?.questionText) return

      const questionText = currentQuestion.questionText.trim()
      if (!questionText) return

      setVoiceError('')
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current = null
      }

      async function playByServerTts() {
        setIsVoiceLoading(true)
        const ttsResult = await voiceApi.textToSpeech({
          text: questionText,
          languageCode: 'ko-KR',
        })
        if (cancelled) return
        if (!ttsResult?.audioUrl) {
          throw createVoiceError('VOICE_TTS_AUDIO_URL_MISSING')
        }
        const audio = new Audio(ttsResult.audioUrl)
        audioRef.current = audio
        try {
          await audio.play()
        } catch (error) {
          if (error?.name === 'NotAllowedError') {
            throw createVoiceError('VOICE_TTS_AUTOPLAY_BLOCKED')
          }
          throw createVoiceError('VOICE_TTS_PLAYBACK_FAILED')
        }
      }

      if (window.speechSynthesis && typeof window.SpeechSynthesisUtterance === 'function') {
        window.speechSynthesis.cancel()
        const utterance = new window.SpeechSynthesisUtterance(questionText)
        utterance.lang = 'ko-KR'
        utterance.rate = 1
        utterance.onerror = async () => {
          if (cancelled) return
          try {
            await playByServerTts()
          } catch (error) {
            if (!cancelled) {
              setVoiceError(getVoiceErrorMessage(error?.code, error?.message))
            }
          } finally {
            if (!cancelled) {
              setIsVoiceLoading(false)
            }
          }
        }
        window.speechSynthesis.speak(utterance)
        return
      }

      try {
        await playByServerTts()
      } catch (error) {
        if (!cancelled) {
          setVoiceError(getVoiceErrorMessage(error?.code, error?.message))
        }
      } finally {
        if (!cancelled) {
          setIsVoiceLoading(false)
        }
      }
    }

    playQuestionVoice()

    return () => {
      cancelled = true
    }
  }, [currentQuestion?.id, currentQuestion?.questionText, isAutoPlayEnabled])

  function moveToNextQuestion(updatedSession) {
    if (!updatedSession) return

    const nextIndex = currentQuestionIndex + 1
    setSession(updatedSession)
    setCurrentQuestionIndex(nextIndex)
    setAnswer(updatedSession.questions[nextIndex]?.answerText ?? '')
    setStatusVariant('success')
    setStatusMessage('\uB2F5\uBCC0\uC774 \uC800\uC7A5\uB418\uACE0 \uB2E4\uC74C \uC9C8\uBB38\uC73C\uB85C \uC774\uB3D9\uD588\uC2B5\uB2C8\uB2E4.')
  }

  async function handleAdvance() {
    if (!session || !currentQuestion || !canAdvance) return

    setIsSubmitting(true)
    setStatusMessage('')
    setPageError('')

    try {
      let updatedSession = session
      const needsSave = Boolean(normalizedAnswer) && normalizedAnswer !== existingAnswer

      if (needsSave) {
        await interviewApi.saveAnswer(sessionId, {
          questionId: currentQuestion.id,
          answerText: normalizedAnswer,
          audioUrl: null,
        })
        updatedSession = await interviewApi.getSession(sessionId)
      }

      if (isLastQuestion) {
        try {
          await interviewApi.endSession(sessionId)
        } catch {
        }
        navigate(`/interview/result?sessionId=${sessionId}`)
        return
      }

      moveToNextQuestion(updatedSession)
    } catch (error) {
      setPageError(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function transcribeRecordedAudio(blob) {
    setIsVoiceLoading(true)
    setVoiceError('')

    try {
      const sttResult = await voiceApi.speechToText(blob, 'ko-KR')
      const transcriptText = sttResult?.transcriptText?.trim() ?? ''
      if (transcriptText.startsWith('Stub transcript for audio:') || sttResult?.stubbed) {
        throw createVoiceError('VOICE_STT_PROVIDER_FAILED', 'Speech-to-text provider returned a stub response.')
      }
      if (!transcriptText) {
        throw createVoiceError('VOICE_STT_EMPTY_TRANSCRIPT')
      }
      setAnswer((previous) => {
        const trimmed = previous.trim()
        return trimmed ? `${trimmed} ${transcriptText}` : transcriptText
      })
      setStatusVariant('success')
      setStatusMessage('음성을 텍스트로 반영했습니다.')
    } catch (error) {
      setVoiceError(getVoiceErrorMessage(error?.code, error?.message))
      setStatusVariant('error')
      setStatusMessage(getVoiceErrorMessage(error?.code, '음성 인식에 실패했습니다.'))
    } finally {
      setIsVoiceLoading(false)
    }
  }

  async function handleVoicePlaceholder() {
    setVoiceError('')

    if (!navigator.mediaDevices?.getUserMedia || typeof window.MediaRecorder === 'undefined') {
      setStatusVariant('error')
      setStatusMessage(getVoiceErrorMessage('VOICE_STT_SERVER_ERROR', '브라우저에서 녹음을 지원하지 않습니다.'))
      return
    }

    if (isRecording && mediaRecorderRef.current) {
      mediaRecorderRef.current.stop()
      setIsRecording(false)
      return
    }

    try {
      const mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaStreamRef.current = mediaStream
      recordedChunksRef.current = []

      const mediaRecorder = new window.MediaRecorder(mediaStream)
      mediaRecorderRef.current = mediaRecorder

      mediaRecorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          recordedChunksRef.current.push(event.data)
        }
      }

      mediaRecorder.onerror = () => {
        setVoiceError(getVoiceErrorMessage('VOICE_STT_RECORDING_EMPTY', '녹음 중 오류가 발생했습니다.'))
        setStatusVariant('error')
        setStatusMessage('녹음을 진행할 수 없습니다.')
      }

      mediaRecorder.onstop = async () => {
        const recordedBlob = new Blob(recordedChunksRef.current, { type: mediaRecorder.mimeType || 'audio/webm' })
        if (mediaStreamRef.current) {
          mediaStreamRef.current.getTracks().forEach((track) => track.stop())
          mediaStreamRef.current = null
        }
        if (recordedBlob.size > 0) {
          await transcribeRecordedAudio(recordedBlob)
        } else {
          setVoiceError(getVoiceErrorMessage('VOICE_STT_RECORDING_EMPTY'))
          setStatusVariant('error')
          setStatusMessage(getVoiceErrorMessage('VOICE_STT_RECORDING_EMPTY'))
        }
      }

      mediaRecorder.start()
      setIsRecording(true)
      setStatusVariant('success')
      setStatusMessage('녹음을 시작했습니다. 다시 누르면 종료됩니다.')
    } catch (error) {
      const permissionCode = error?.name === 'NotAllowedError'
        ? 'VOICE_STT_MIC_PERMISSION_DENIED'
        : 'VOICE_STT_SERVER_ERROR'
      setVoiceError(getVoiceErrorMessage(permissionCode, error?.message))
      setStatusVariant('error')
      setStatusMessage(getVoiceErrorMessage(permissionCode, '녹음을 시작하지 못했습니다.'))
    }
  }

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label={STATUS_MESSAGES.loadingInterviewSession} /></section>
  }

  if (pageError && !session) {
    return <section className="workspace-page"><ErrorBlock message={pageError} /></section>
  }

  if (!currentQuestion) {
    return (
      <section className="workspace-page">
        <div className="workspace-page__hero">
          <p className="page-card__eyebrow">{'\uBA74\uC811 \uC138\uC158'}</p>
          <h2 className="page-card__title">{'\uC9C4\uD589 \uC911\uC778 \uC9C8\uBB38\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.'}</h2>
          <p className="page-card__description">{'\uBA74\uC811 \uC124\uC815 \uD654\uBA74\uC5D0\uC11C \uC0C8 \uC138\uC158\uC744 \uC2DC\uC791\uD574 \uC8FC\uC138\uC694.'}</p>
        </div>
        <div className="button-row">
          <button className="button" type="button" onClick={() => navigate('/interview/setup')}>{BUTTON_LABELS.goToSetup}</button>
        </div>
      </section>
    )
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">{'\uBA74\uC811 \uC9C4\uD589'}</p>
        <h2 className="page-card__title">{'\uC9C8\uBB38\uC5D0 \uB2F5\uD558\uACE0 \uB2E4\uC74C \uC9C8\uBB38\uC73C\uB85C \uCC28\uADFC\uCC28\uADFC \uC9C4\uD589\uD574 \uBCF4\uC138\uC694.'}</h2>
        <p className="page-card__description">{`\uBAA8\uB4DC: ${modeLabel} \u00B7 \uC644\uB8CC\uC728 ${session?.completionRate ?? 0}% \u00B7 \uB2F5\uBCC0 ${session?.answeredQuestions ?? 0}/${session?.totalQuestions ?? 0}`}</p>
        {sessionSource ? <p className="page-card__description">{`\uC9C8\uBB38 \uC0DD\uC131 \uC18C\uC2A4: ${sessionSource}`}</p> : null}
      </div>

      <StatusMessage variant="error" message={session ? pageError : ''} />
      <StatusMessage variant={statusVariant} message={statusMessage} />

      <div className="workspace-page">
        <QuestionCard question={currentQuestion} index={currentQuestionIndex} total={session.questions.length} mode={modeLabel} />
        <AnswerInput
          answer={answer}
          onChange={setAnswer}
          onSubmit={handleAdvance}
          onRecordPlaceholder={handleVoicePlaceholder}
          isSubmitting={isSubmitting}
          submitLabel={isLastQuestion ? BUTTON_LABELS.viewResult : BUTTON_LABELS.nextQuestion}
          isSubmitDisabled={!canAdvance}
          isRecording={isRecording}
          isVoiceLoading={isVoiceLoading}
          voiceError={voiceError}
          isAutoPlayEnabled={isAutoPlayEnabled}
          onToggleAutoPlay={() => setIsAutoPlayEnabled((previous) => !previous)}
        />
        <FeedbackCard
          feedback={feedbackMessage}
          title={'\uD604\uC7AC \uD53C\uB4DC\uBC31'}
          description={'\uC800\uC7A5\uD55C \uB2F5\uBCC0\uC744 \uAE30\uBC18\uC73C\uB85C \uC9C0\uAE08\uAE4C\uC9C0\uC758 \uAC1C\uC120 \uBC29\uD5A5\uC744 \uBCF4\uC5EC\uB4DC\uB9BD\uB2C8\uB2E4.'}
        />
        <div className="button-row">
          <button className="button button--secondary" type="button" onClick={() => navigate('/dashboard')}>{BUTTON_LABELS.goToDashboard}</button>
        </div>
      </div>
    </section>
  )
}

export default InterviewSessionPage
