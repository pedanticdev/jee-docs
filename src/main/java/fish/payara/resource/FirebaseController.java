package fish.payara.resource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.omnifaces.cdi.Eager;

import javax.annotation.PostConstruct;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;



@ApplicationScoped
public class FirebaseController {
    static final Logger LOGGER = Logger.getLogger(FirebaseController.class.getName());


    @Inject
    EmbeddingService embeddingService;

    @Inject
    @ConfigProperty(name = "embedding.timer.initial.delay.minutes", defaultValue = "0")
    private long initialDelayMinutes;

    @Inject
    @ConfigProperty(name = "embedding.timer.interval.minutes", defaultValue = "30")
    private long intervalMinutes;
    final Timer timer = new Timer();


    void contextInitialized(@Observes @Initialized(ApplicationScoped.class) Object event) {
        init();
    }


    public void init() {
            timer.scheduleAtFixedRate(
                    new TimerTask() {
                        @Override
                        public void run() {
                           embeddingService.embedNewDocs();
                        }
                    },
                    initialDelayMinutes,
                    intervalMinutes * 1000L);

    }



}


