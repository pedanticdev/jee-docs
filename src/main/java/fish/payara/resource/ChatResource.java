package fish.payara.resource;

import com.drew.lang.annotations.NotNull;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.logging.Logger;

@Path("chat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ChatResource {

    static final Logger LOGGER = Logger.getLogger(ChatResource.class.getName());

    @Inject
    private ChatService chatService;

    @POST
    public Response post(@NotNull ChatRequest chatRequest) {
        ChatResponse chat = chatService.chat(chatRequest);
        LOGGER.info("Chat request received: " + chat);
       return Response.ok(chat).build();
    }
}
