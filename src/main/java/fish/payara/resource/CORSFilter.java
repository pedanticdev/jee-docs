package fish.payara.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class CORSFilter implements ContainerResponseFilter {
    @Inject
    @ConfigProperty(name = "env", defaultValue = "dev")
    String env;
    @Inject
    @ConfigProperty(name = "allowed.origins", defaultValue = "*")
    String allowedOrigins;

    public static final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS, HEAD";
    public final static int MAX_AGE = 42 * 60 * 60;
    public final static String DEFAULT_ALLOWED_HEADERS = "origin,accept,content-type,authorization,x-requested-with,access-control-request-method,access-control-request-headers";
    public final static String DEFAULT_EXPOSED_HEADERS = "location,info";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Logger.getLogger(CORSFilter.class.getSimpleName()).log(Level.INFO, "CORS filter enabled. Allowed origins: {0}", allowedOrigins);
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        // Always add the Origin header
        headers.add("Access-Control-Allow-Origin", allowedOrigins);
        headers.add("Access-Control-Allow-Credentials", "true");

        // Handle preflight
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            headers.add("Access-Control-Allow-Methods", ALLOWED_METHODS);
            headers.add("Access-Control-Allow-Headers", DEFAULT_ALLOWED_HEADERS);
            headers.add("Access-Control-Max-Age", "1209600");
            responseContext.setStatus(Response.Status.OK.getStatusCode());
            return;
        }

        // Non-preflight requests
        headers.add("Access-Control-Expose-Headers", DEFAULT_EXPOSED_HEADERS);
    }

    private boolean isProd() {
        return env.equalsIgnoreCase("prod");
    }

    String getRequestedAllowedHeaders(ContainerRequestContext responseContext) {
        List<String> headers = responseContext.getHeaders().get("Access-Control-Request-Headers");
        return createHeaderList(headers, DEFAULT_ALLOWED_HEADERS);
    }

    String getRequestedExposedHeaders(ContainerRequestContext responseContext) {
        List<String> headers = responseContext.getHeaders().get("Access-Control-Expose-Headers");
        return createHeaderList(headers, DEFAULT_EXPOSED_HEADERS);
    }

    String createHeaderList(List<String> headers, String defaultHeaders) {
        if (headers == null || headers.isEmpty()) {
            return defaultHeaders;
        }
        StringBuilder retVal = new StringBuilder();
        for (String s : headers) {
            retVal.append(s);
            retVal.append(',');
        }
        retVal.append(defaultHeaders);
        return retVal.toString();
    }
}
