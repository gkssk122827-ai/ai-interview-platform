from fastapi import APIRouter, HTTPException, Request

from schemas.learning import (
    LearningGenerateRequest,
    LearningGenerateResponse,
    LearningGradeRequest,
    LearningGradeResponse,
)


router = APIRouter(prefix="/learning", tags=["learning"])


@router.post("/generate", response_model=LearningGenerateResponse, response_model_exclude_none=True)
async def generate_learning_problems(
    request: Request,
    payload: LearningGenerateRequest,
) -> LearningGenerateResponse:
    try:
        learning_service = request.app.state.learning_service
        return await learning_service.generate_problems(payload)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail={
                "code": "LEARNING_GENERATE_ERROR",
                "message": f"Failed to generate learning problems: {exc}",
            },
        ) from exc


@router.post("/grade", response_model=LearningGradeResponse)
async def grade_learning_answer(
    request: Request,
    payload: LearningGradeRequest,
) -> LearningGradeResponse:
    try:
        learning_service = request.app.state.learning_service
        return await learning_service.grade_answer(payload)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail={
                "code": "LEARNING_GRADE_ERROR",
                "message": f"Failed to grade learning answer: {exc}",
            },
        ) from exc
