package com.comsenity.geecon.genai.scoring.a2a;

import com.embabel.agent.a2a.server.AgentCardHandler;
import io.a2a.spec.AgentCard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Embabel's {@code embabel-agent-starter-a2a} serves the agent card at
 * {@code GET /a2a/.well-known/agent.json} (older A2A 0.2.x spec path, prefixed under the
 * {@code a2a} path). The langchain4j-agentic-a2a client (which the coordinator uses) follows the
 * newer A2A 0.3.x spec and fetches {@code GET /.well-known/agent-card.json} at the root.
 *
 * <p>This tiny alias exposes the Embabel-derived {@link AgentCard} at the root path the
 * langchain4j client expects. The card's {@code url} field is built by Embabel and points at the
 * proper {@code /a2a} JSON-RPC endpoint, so subsequent POSTs from the client land on Embabel's
 * native handler.
 */
@RestController
public class AgentCardAlias {

    private final AgentCardHandler agentCardHandler;

    public AgentCardAlias(AgentCardHandler agentCardHandler) {
        this.agentCardHandler = agentCardHandler;
    }

    @GetMapping("/.well-known/agent-card.json")
    public AgentCard agentCard(HttpServletRequest request) {
        return agentCardHandler.agentCard(
                request.getScheme(),
                request.getServerName(),
                request.getServerPort());
    }
}
