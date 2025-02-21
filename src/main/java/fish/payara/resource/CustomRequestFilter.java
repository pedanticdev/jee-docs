package fish.payara.resource;

import com.google.firebase.auth.FirebaseAuthException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;


@Provider
@Priority(Priorities.AUTHENTICATION)
public class CustomRequestFilter implements ContainerRequestFilter {
    static final Logger LOGGER = Logger.getLogger(CustomRequestFilter.class.getName());
    @Inject
    AuthController authController;

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        LOGGER.log(Level.FINE, "Entering CustomerRequestFilter filter");
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("No token provided")
                            .build()
            );
            return;
        }

            String token = authHeader.substring(7);
            try {
                authController.addUserToRole(token);
            } catch (FirebaseAuthException e) {
                LOGGER.log(Level.SEVERE, "Auth error: ", e);
                requestContext.abortWith(
                        Response.status(Response.Status.FORBIDDEN)
                                .entity("Invalid token")
                                .build()
                );

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error: ", e);
                requestContext.abortWith(
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Server error")
                                .build()
                );
            }

    }
}
