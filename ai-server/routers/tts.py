from fastapi import APIRouter, Request

from schemas.tts import TextToSpeechRequest, TextToSpeechResponse


router = APIRouter(tags=["tts"])


@router.post("/tts", response_model=TextToSpeechResponse)
async def text_to_speech(request: Request, payload: TextToSpeechRequest) -> TextToSpeechResponse:
    tts_service = request.app.state.tts_service
    audio_url = await tts_service.synthesize(payload.text, payload.voiceName)
    return TextToSpeechResponse(audioUrl=audio_url, providerName="openai-tts")

