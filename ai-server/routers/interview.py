from fastapi import APIRouter, HTTPException, Request

from schemas.interview import (
    InterviewFeedbackRequest,
    InterviewFeedbackResponse,
    InterviewQuestionRequest,
    InterviewQuestionResponse,
)


router = APIRouter(prefix="/interview", tags=["interview"])


@router.post("/question", response_model=InterviewQuestionResponse)
async def generate_question(
    request: Request,
    payload: InterviewQuestionRequest,
) -> InterviewQuestionResponse:
    try:
        interview_service = request.app.state.interview_service
        return await interview_service.generate_question(payload)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail={
                "code": "INTERVIEW_QUESTION_ERROR",
                "message": f"Failed to generate interview question: {exc}",
            },
        ) from exc


@router.post("/feedback", response_model=InterviewFeedbackResponse)
async def generate_feedback(
    request: Request,
    payload: InterviewFeedbackRequest,
) -> InterviewFeedbackResponse:
    try:
        interview_service = request.app.state.interview_service
        return await interview_service.generate_feedback(payload)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail={
                "code": "INTERVIEW_FEEDBACK_ERROR",
                "message": f"Failed to generate interview feedback: {exc}",
            },
        ) from exc
