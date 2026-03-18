import json
from pathlib import Path

from fastapi import HTTPException
from langchain_core.prompts import PromptTemplate
from langchain_openai import ChatOpenAI

from schemas.interview import (
    InterviewFeedbackRequest,
    InterviewFeedbackResponse,
    InterviewQuestionRequest,
    InterviewQuestionResponse,
)


class InterviewService:
    def __init__(self, api_key: str | None, model_name: str) -> None:
        self.api_key = api_key
        self.model_name = model_name
        self.prompt_dir = Path(__file__).resolve().parent.parent / "prompts"

    async def generate_question(self, payload: InterviewQuestionRequest) -> InterviewQuestionResponse:
        llm = self._create_llm()
        template = self._load_prompt("interview_question.txt")

        # The prompt is structured to force a single, interviewer-style question
        # grounded in candidate materials and prior answers.
        prompt = PromptTemplate.from_template(template)
        chain = prompt | llm
        response = await chain.ainvoke(
            {
                "resume_content": payload.resumeContent or "",
                "cover_letter_content": payload.coverLetterContent or "",
                "job_description": payload.jobDescription or "",
                "conversation_history": self._format_history(payload.conversationHistory),
            }
        )

        data = self._parse_json_response(response.content, "INTERVIEW_QUESTION_ERROR")
        return InterviewQuestionResponse.model_validate(data)

    async def generate_feedback(self, payload: InterviewFeedbackRequest) -> InterviewFeedbackResponse:
        llm = self._create_llm()
        template = self._load_prompt("interview_feedback.txt")

        # The prompt is designed to return structured scoring and actionable coaching
        # instead of free-form narrative so the backend can consume it safely.
        prompt = PromptTemplate.from_template(template)
        chain = prompt | llm
        response = await chain.ainvoke(
            {
                "conversation_history": self._format_history(payload.conversationHistory),
            }
        )

        data = self._parse_json_response(response.content, "INTERVIEW_FEEDBACK_ERROR")
        return InterviewFeedbackResponse.model_validate(data)

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

    def _format_history(self, history: list) -> str:
        if not history:
            return "No prior conversation."

        lines: list[str] = []
        for index, turn in enumerate(history, start=1):
            lines.append(f"{index}. Question: {turn.question}")
            lines.append(f"{index}. Answer: {turn.answer}")
        return "\n".join(lines)

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
