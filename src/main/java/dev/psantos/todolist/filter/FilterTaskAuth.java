package dev.psantos.todolist.filter;

import java.io.IOException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import at.favre.lib.crypto.bcrypt.BCrypt;
import dev.psantos.todolist.user.IUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FilterTaskAuth extends OncePerRequestFilter {
    @Autowired
    private IUserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (authenticate(request)) {
            filterChain.doFilter(request, response);
        } else {
            response.sendError(401);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws SecurityException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        return (path.equals("/live/websocket") && method.equals("GET"))
                || (path.equals("/users") && method.equals("POST"));
    }

    private String[] getCredentials(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic")) {
            return null;
        }

        var encodedAuth = authorizationHeader.substring("Basic".length()).trim();
        byte[] decodedAuth = Base64.getDecoder().decode(encodedAuth);
        var authString = new String(decodedAuth);

        return authString.split(":");
    }

    private boolean authenticate(HttpServletRequest request) {
        var credentials = getCredentials(request);

        if (credentials == null) {
            return false;
        }

        String username = credentials[0];
        String password = credentials[1];

        var user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }

        var passwordVerify = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());
        if (passwordVerify.verified) {
            return true;
        } else {
            return false;
        }
    }

}
