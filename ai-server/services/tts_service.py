import base64

from fastapi import HTTPException
from openai import OpenAI


class TextToSpeechService:
    def __init__(self, api_key: str | None, model_name: str) -> None:
        self.api_key = api_key
        self.model_name = model_name

    async def synthesize(self, text: str, voice_name: str) -> str:
        if not self.api_key:
            raise HTTPException(
                status_code=500,
                detail={
                    "code": "MISSING_OPENAI_API_KEY",
                    "message": "OPENAI_API_KEY is not configured.",
                },
            )

        try:
            client = OpenAI(api_key=self.api_key)
            response = client.audio.speech.create(
                model=self.model_name or "gpt-4o-mini-tts",
                voice=voice_name or "alloy",
                input=text,
                response_format="mp3",
            )

            if hasattr(response, "read"):
                audio_bytes = response.read()
            else:
                audio_bytes = getattr(response, "content", None)

            if not audio_bytes:
                raise HTTPException(
                    status_code=500,
                    detail={
                        "code": "TTS_EMPTY_AUDIO",
                        "message": "TTS provider returned empty audio bytes.",
                    },
                )

            encoded_audio = base64.b64encode(audio_bytes).decode("ascii")
            return f"data:audio/mpeg;base64,{encoded_audio}"
        except HTTPException:
            raise
        except Exception as exc:
            raise HTTPException(
                status_code=500,
                detail={
                    "code": "TTS_PROVIDER_ERROR",
                    "message": f"Failed to generate speech audio: {exc}",
                },
            ) from exc
