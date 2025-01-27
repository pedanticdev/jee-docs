package fish.payara.resource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import static java.time.Duration.ofSeconds;

@ApplicationScoped
public class OpenAIFactory {
    static final Logger LOGGER = Logger.getLogger(OpenAIFactory.class.getName());



    @Inject
    @ConfigProperty(name = "open.api.key")
    String apiKey;


    @Inject
    @ConfigProperty(name = "model.temperature", defaultValue = "0.7")
    Double temperature;




    @Produces
    @Singleton
    public OpenAiChatModel produceModelGpt4Model() {
        return builder().modelName(OpenAiChatModelName.GPT_4_O).build();
    }


    private OpenAiChatModel.OpenAiChatModelBuilder builder() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .temperature(temperature)
                .timeout(ofSeconds(60))
                .logRequests(true)
                .logResponses(true);
    }

    private InputStream readAuthFile() {
        return FirebaseController.class.getClassLoader().getResourceAsStream("/firebase-auth.json");
    }


    @Produces
    @Singleton
    public Storage getStorage() {
        InputStream resourceAsStream = readAuthFile();
        try {
            return StorageOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(
                            resourceAsStream))
                    .build()
                    .getService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
