from contextlib import asynccontextmanager
import os

from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from routers.interview import router as interview_router
from routers.learning import router as learning_router
from routers.stt import router as stt_router
from services.interview_service import InterviewService
from services.learning_service import LearningService
from services.whisper_service import WhisperService


load_dotenv()


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.whisper_service = WhisperService(
        api_key=os.getenv("OPENAI_API_KEY"),
        model_name=os.getenv("MODEL_NAME", "gpt-4o"),
    )
    app.state.interview_service = InterviewService(
        api_key=os.getenv("OPENAI_API_KEY"),
        model_name=os.getenv("MODEL_NAME", "gpt-4o"),
    )
    app.state.learning_service = LearningService(
        api_key=os.getenv("OPENAI_API_KEY"),
        model_name="gpt-4o",
    )
    yield


app = FastAPI(title="AI Interview Platform AI Server", lifespan=lifespan)
app.include_router(stt_router)
app.include_router(interview_router)
app.include_router(learning_router)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException) -> JSONResponse:
    detail = exc.detail if isinstance(exc.detail, dict) else {"code": "HTTP_ERROR", "message": str(exc.detail)}
    return JSONResponse(status_code=exc.status_code, content={"detail": detail})


@app.exception_handler(Exception)
async def unhandled_exception_handler(_: Request, exc: Exception) -> JSONResponse:
    return JSONResponse(
        status_code=500,
        content={
            "detail": {
                "code": "INTERNAL_SERVER_ERROR",
                "message": str(exc) or "An unexpected server error occurred.",
            }
        },
    )
