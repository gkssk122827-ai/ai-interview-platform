from typing import Literal

from pydantic import BaseModel, Field


class ConversationTurn(BaseModel):
    question: str = Field(..., min_length=1)
    answer: str = Field(..., min_length=1)


class InterviewQuestionRequest(BaseModel):
    resumeContent: str = ""
    coverLetterContent: str = ""
    jobDescription: str = ""
    interviewMode: str = "COMPREHENSIVE"
    positionCategory: str = "BACKEND"
    questionDifficulty: str = "MEDIUM"
    questionIndex: int = Field(1, ge=1)
    totalQuestionCount: int = Field(1, ge=1)
    modeGuide: str = ""
    existingQuestions: list[str] = Field(default_factory=list)
    conversationHistory: list[ConversationTurn] = Field(default_factory=list)


class InterviewQuestionResponse(BaseModel):
    question: str
    questionType: Literal["INITIAL", "FOLLOWUP"]


class InterviewFeedbackRequest(BaseModel):
    conversationHistory: list[ConversationTurn] = Field(default_factory=list)


class InterviewFeedbackResponse(BaseModel):
    logicScore: int = Field(..., ge=0, le=100)
    relevanceScore: int = Field(..., ge=0, le=100)
    specificityScore: int = Field(..., ge=0, le=100)
    overallScore: int = Field(..., ge=0, le=100)
    weakPoints: str
    improvements: str
    recommendedAnswer: str
