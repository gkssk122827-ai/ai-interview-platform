package com.aimentor.external.ai;

/**
 * Represents one interview QA turn exchanged with the external AI service.
 */
public record ConversationTurnDto(
        String question,
        String answer
) {
}
