from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class LearningProblem(BaseModel):
    type: Literal["MULTIPLE", "SHORT"]
    question: str = Field(..., min_length=1)
    choices: list[str] | None = None
    answer: str = Field(..., min_length=1)
    explanation: str = Field(..., min_length=1)

    @model_validator(mode="after")
    def validate_problem_shape(self) -> "LearningProblem":
        if self.type == "MULTIPLE":
            if not self.choices or len(self.choices) != 4:
                raise ValueError("MULTIPLE problems must include exactly 4 choices.")
            if self.answer not in self.choices:
                raise ValueError("MULTIPLE problem answers must match one of the choices.")
        if self.type == "SHORT" and self.choices is not None:
            raise ValueError("SHORT problems must not include choices.")
        return self


class LearningGenerateRequest(BaseModel):
    subject: str = Field(..., min_length=1)
    difficulty: Literal["EASY", "MEDIUM", "HARD"]
    count: int = Field(..., ge=1, le=10)
    type: Literal["MULTIPLE", "SHORT", "MIX"]


class LearningGenerateResponse(BaseModel):
    problems: list[LearningProblem]


class LearningGradeRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    question: str = Field(..., min_length=1)
    correct_answer: str = Field(..., alias="correctAnswer", min_length=1)
    user_answer: str = Field(..., alias="userAnswer", min_length=1)
    explanation: str = Field(..., min_length=1)


class LearningGradeResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    is_correct: bool = Field(..., alias="isCorrect")
    ai_feedback: str = Field(..., alias="aiFeedback", min_length=1)
