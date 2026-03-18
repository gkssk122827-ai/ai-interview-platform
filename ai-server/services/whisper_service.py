import os
import tempfile
from pathlib import Path

from fastapi import HTTPException
from openai import OpenAI


class WhisperService:
    def __init__(self, api_key: str | None, model_name: str) -> None:
        self.api_key = api_key
        self.model_name = model_name

    async def transcribe_bytes(self, filename: str, content: bytes) -> str:
        if not self.api_key:
            raise HTTPException(
                status_code=500,
                detail={
                    "code": "MISSING_OPENAI_API_KEY",
                    "message": "OPENAI_API_KEY is not configured.",
                },
            )

        suffix = Path(filename).suffix or ".webm"
        temp_path: str | None = None

        try:
            with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
                temp_file.write(content)
                temp_path = temp_file.name

            client = OpenAI(api_key=self.api_key)

            # Actual OpenAI Whisper API call happens here.
            with open(temp_path, "rb") as audio_file:
                response = client.audio.transcriptions.create(
                    model="whisper-1",
                    file=audio_file,
                    language="ko",
                )

            text = getattr(response, "text", None)
            if not text:
                raise HTTPException(
                    status_code=500,
                    detail={
                        "code": "WHISPER_API_ERROR",
                        "message": "Whisper returned an empty transcription result.",
                    },
                )

            return text
        except HTTPException:
            raise
        except Exception as exc:
            raise HTTPException(
                status_code=500,
                detail={
                    "code": "WHISPER_API_ERROR",
                    "message": f"Failed to transcribe audio with OpenAI Whisper: {exc}",
                },
            ) from exc
        finally:
            if temp_path and os.path.exists(temp_path):
                os.remove(temp_path)
