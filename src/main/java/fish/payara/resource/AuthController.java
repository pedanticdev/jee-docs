package fish.payara.resource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class AuthController {
    public static final String CHAT_USER_ROLE = "chat_user";
    public static final String ADMIN_ROLE = "admin";
    public static final String ROLES = "roles";
    static final Logger LOGGER = Logger.getLogger(AuthController.class.getName());


    private void initFirebase() {
        LOGGER.info("Initializing Firebase");
        List<FirebaseApp> apps = FirebaseApp.getApps();
        if (apps.isEmpty()) {
            LOGGER.log(Level.INFO, "No Firebase apps found. Initializing default app");
            try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("/firebase-auth.json")) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(resourceAsStream))
                        .setStorageBucket("jakarta-ee-adc2a.appspot.com")
                        .build();
                FirebaseApp.initializeApp(options);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }



    public void addUserToRole(String uid, String role) {
        LOGGER.log(Level.INFO, "FirebaseApp initialized");
        try {
            // Set custom claims on the user
            FirebaseAuth.getInstance().setCustomUserClaims(uid, Map.of("role", role));
            LOGGER.log(Level.INFO, String.format("Firebase user %s added to role %s", uid, role));

        } catch (FirebaseAuthException e) {
            System.err.println("Error adding custom claims: " + e.getMessage());
        }

    }

    public void addUserToRole(String token) throws FirebaseAuthException {
        initFirebase();
        FirebaseToken firebaseToken = FirebaseAuth.getInstance().verifyIdToken(token);
        boolean hasRole = firebaseToken.getClaims().containsKey(ROLES) &&
                          ((List<String>) firebaseToken.getClaims().get(ROLES)).contains(CHAT_USER_ROLE);
        if (!hasRole) {
            addUserToRole(firebaseToken.getUid(), CHAT_USER_ROLE);
            if ("sinaisix@gmail.com".equals(firebaseToken.getEmail())) {
                addUserToRole(firebaseToken.getUid(), ADMIN_ROLE);

            }
        }

    }
}
