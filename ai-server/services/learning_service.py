import json
from pathlib import Path

from fastapi import HTTPException
from langchain_core.prompts import PromptTemplate
from langchain_openai import ChatOpenAI
from pydantic import ValidationError

from schemas.learning import (
    LearningGenerateRequest,
    LearningGenerateResponse,
    LearningGradeRequest,
    LearningGradeResponse,
)


class LearningService:
    SUBJECT_PROMPTS = {
        "영어": "learning_generate_en.txt",
        "english": "learning_generate_en.txt",
        "en": "learning_generate_en.txt",
        "국사": "learning_generate_history.txt",
        "한국사": "learning_generate_history.txt",
        "history": "learning_generate_history.txt",
    }

    def __init__(self, api_key: str | None, model_name: str = "gpt-4o") -> None:
        self.api_key = api_key
        self.model_name = model_name or "gpt-4o"
        self.prompt_dir = Path(__file__).resolve().parent.parent / "prompts"

    async def generate_problems(self, payload: LearningGenerateRequest) -> LearningGenerateResponse:
        llm = self._create_llm()
        template = self._load_prompt(self._resolve_generate_prompt(payload.subject))

        # The generation prompt is subject-specific so the model stays anchored
        # to the right curriculum while still returning a strict JSON payload.
        prompt = PromptTemplate.from_template(template)
        chain = prompt | llm
        response = await chain.ainvoke(
            {
                "difficulty": payload.difficulty,
                "count": payload.count,
                "problem_type": payload.type,
            }
        )

        data = self._parse_json_response(response.content, "LEARNING_GENERATE_ERROR")
        try:
            result = LearningGenerateResponse.model_validate(data)
        except ValidationError as exc:
            raise HTTPException(
                status_code=500,
                detail={
                    "code": "LEARNING_GENERATE_ERROR",
                    "message": f"Model returned invalid learning problem schema: {exc}",
                },
            ) from exc

        if len(result.problems) != payload.count:
            raise HTTPException(
                status_code=500,
                detail={
                    "code": "LEARNING_GENERATE_ERROR",
                    "message": f"Model returned {len(result.problems)} problems instead of {payload.count}.",
                },
            )
        return result

    async def grade_answer(self, payload: LearningGradeRequest) -> LearningGradeResponse:
        llm = self._create_llm()
        template = self._load_prompt("learning_grade.txt")

        # The grading prompt separates correctness from explanation so the model
        # returns deterministic pass/fail data plus actionable feedback text.
        prompt = PromptTemplate.from_template(template)
        chain = prompt | llm
        response = await chain.ainvoke(
            {
                "question": payload.question,
                "correct_answer": payload.correct_answer,
                "user_answer": payload.user_answer,
                "explanation": payload.explanation,
            }
        )

        data = self._parse_json_response(response.content, "LEARNING_GRADE_ERROR")
        try:
            return LearningGradeResponse.model_validate(data)
        except ValidationError as exc:
            raise HTTPException(
                status_code=500,
                detail={
                    "code": "LEARNING_GRADE_ERROR",
                    "message": f"Model returned invalid grading schema: {exc}",
                },
            ) from exc

    def _resolve_generate_prompt(self, subject: str) -> str:
        normalized = subject.strip().lower()
        prompt_name = self.SUBJECT_PROMPTS.get(normalized)
        if not prompt_name:
            raise HTTPException(
                status_code=400,
                detail={
                    "code": "UNSUPPORTED_SUBJECT",
                    "message": f"Unsupported subject: {subject}. Supported subjects are 영어, 국사.",
                },
            )
        return prompt_name

    def _create_llm(self) -> ChatOpenAI:
        if not self.api_key:
            raise HTTPException(
                status_code=500,
                detail={
                    "code": "MISSING_OPENAI_API_KEY",
                    "message": "OPENAI_API_KEY is not configured.",
                },
            )

        return ChatOpenAI(
            api_key=self.api_key,
            model=self.model_name or "gpt-4o",
            model_kwargs={"response_format": {"type": "json_object"}},
            temperature=0.4,
        )

    def _load_prompt(self, filename: str) -> str:
        return (self.prompt_dir / filename).read_text(encoding="utf-8")

    def _parse_json_response(self, content: str, error_code: str) -> dict:
        try:
            return json.loads(content)
        except json.JSONDecodeError as exc:
            raise HTTPException(
                status_code=500,
                detail={
                    "code": error_code,
                    "message": f"Model returned invalid JSON: {content}",
                },
            ) from exc
