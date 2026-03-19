from pydantic import BaseModel, Field


class TextToSpeechRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=5000)
    voiceName: str = Field(default="alloy", max_length=50)
    languageCode: str = Field(default="ko-KR", max_length=20)


class TextToSpeechResponse(BaseModel):
    audioUrl: str
    providerName: str

