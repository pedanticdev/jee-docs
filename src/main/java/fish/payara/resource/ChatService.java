package fish.payara.resource;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChatService {

    @Inject
    OpenAiChatModel model;

    @Inject PostgresEmbeddingService postgresEmbeddingService;

    ChatInterface chatInterface;


    @PostConstruct
    void init() {
        ContentRetriever contentRetriever = postgresEmbeddingService.getContentRetriever();
        chatInterface =
                AiServices.builder(ChatInterface.class)
                        .chatLanguageModel(model)
                        .contentRetriever(contentRetriever)
                        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                        .build();

    }



    public ChatResponse chat(ChatRequest chatRequest) {
        Result<AiMessage> chat = chatInterface.ask(chatRequest.userMessage());
        return new ChatResponse(chat.content().text(), chatRequest);
    }

}
