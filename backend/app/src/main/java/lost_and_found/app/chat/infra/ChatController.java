package lost_and_found.app.chat.infra;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lost_and_found.app.chat.dto.requests.CreateChatThreadDTO;
import lost_and_found.app.chat.dto.requests.SendChatMessageDTO;
import lost_and_found.app.chat.dto.responses.ChatMessageResponseDTO;
import lost_and_found.app.chat.dto.responses.ChatThreadSummaryDTO;
import lost_and_found.app.chat.dto.responses.WsTicketResponseDTO;
import lost_and_found.app.chat.service.ChatService;
import lost_and_found.app.auth.service.WsTicketService;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.shared.domain.User;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final WsTicketService wsTicketService;

    public ChatController(ChatService chatService, WsTicketService wsTicketService) {
        this.chatService = chatService;
        this.wsTicketService = wsTicketService;
    }

    /** Exchanges the caller's session for a short-lived ticket that opens the chat WebSocket. */
    @PostMapping("/ws-ticket")
    public ResponseEntity<WsTicketResponseDTO> createWsTicket(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(new WsTicketResponseDTO(wsTicketService.issue(user)));
    }

    @GetMapping("/threads")
    public ResponseEntity<List<ChatThreadSummaryDTO>> getThreads(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) Long itemId
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        
        if (itemId != null) {
            return ResponseEntity.ok(chatService.getThreadsForItem(user.getId(), itemId));
        }
        
        return ResponseEntity.ok(chatService.getThreadsForUser(user.getId()));
    }

    @PostMapping("/threads")
    public ResponseEntity<ChatThreadSummaryDTO> openThread(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateChatThreadDTO req
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(chatService.openThread(user.getId(), req));
    }

    /** Contact the poster of an item. Used by "Contact" buttons, so the client never sends a poster id. */
    @PostMapping("/items/{itemId}/thread")
    public ResponseEntity<ChatThreadSummaryDTO> openThreadForItem(
        @AuthenticationPrincipal User user,
        @PathVariable Long itemId
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(chatService.openThreadForItem(user.getId(), itemId));
    }

    @GetMapping("/threads/{threadId}/messages")
    public ResponseEntity<List<ChatMessageResponseDTO>> getMessages(
        @AuthenticationPrincipal User user,
        @PathVariable Long threadId
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(chatService.getMessages(user.getId(), threadId));
    }

    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<ChatMessageResponseDTO> sendMessage(
        @AuthenticationPrincipal User user,
        @PathVariable Long threadId,
        @Valid @RequestBody SendChatMessageDTO req
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(chatService.sendMessage(user.getId(), threadId, req));
    }
}
