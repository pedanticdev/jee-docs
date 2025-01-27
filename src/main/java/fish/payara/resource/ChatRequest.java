package fish.payara.resource;

public record ChatRequest(String userMessage, ChatRequestMetadata metadata) {
}
