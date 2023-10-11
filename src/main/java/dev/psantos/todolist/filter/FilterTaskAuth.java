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
        // Pegar a autenticação
        var authorizationHeader = request.getHeader("Authorization");
        var encodedAuth = authorizationHeader.substring("Basic".length()).trim();
        byte[] decodedAuth = Base64.getDecoder().decode(encodedAuth);
        var authString = new String(decodedAuth);

        String[] credentials = authString.split(":");
        String username = credentials[0];
        String password = credentials[1];

        // Validar usuário
        var user = this.userRepository.findByUsername(username);

        if (user == null) {
            response.sendError(401);
        } else {
            // Validar a senha
            var passwordVerify = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());
            if (passwordVerify.verified) {
                filterChain.doFilter(request, response);
            } else {
                response.sendError(401);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws SecurityException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.equals("/live/websocket") && method.equals("GET")) {
            return true;
        }

        if (path.equals("/users") && method.equals("POST")) {
            return true;
        }

        return false;
    }

}
