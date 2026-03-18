from fastapi import APIRouter, File, HTTPException, Request, UploadFile

from schemas.stt import SpeechToTextResponse


router = APIRouter(tags=["stt"])

SUPPORTED_EXTENSIONS = {"webm", "wav", "mp3", "mp4", "m4a"}
MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024


def _extract_extension(filename: str | None) -> str:
    if not filename or "." not in filename:
        return ""
    return filename.rsplit(".", 1)[-1].lower()


async def _transcribe(request: Request, audio: UploadFile) -> SpeechToTextResponse:
    extension = _extract_extension(audio.filename)
    if extension not in SUPPORTED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail={
                "code": "UNSUPPORTED_FILE_TYPE",
                "message": "Supported formats are: webm, wav, mp3, mp4, m4a.",
            },
        )

    content = await audio.read()
    if len(content) > MAX_FILE_SIZE_BYTES:
        raise HTTPException(
            status_code=400,
            detail={
                "code": "FILE_TOO_LARGE",
                "message": "Maximum file size is 25MB.",
            },
        )

    whisper_service = request.app.state.whisper_service
    text = await whisper_service.transcribe_bytes(filename=audio.filename or "audio", content=content)
    return SpeechToTextResponse(text=text)


@router.post("/speech/stt", response_model=SpeechToTextResponse)
async def speech_to_text(request: Request, audio: UploadFile = File(...)) -> SpeechToTextResponse:
    return await _transcribe(request, audio)


@router.post("/stt", response_model=SpeechToTextResponse)
async def speech_to_text_alias(request: Request, audio: UploadFile = File(...)) -> SpeechToTextResponse:
    return await _transcribe(request, audio)
