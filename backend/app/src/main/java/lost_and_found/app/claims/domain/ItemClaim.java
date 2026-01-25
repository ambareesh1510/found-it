package lost_and_found.app.claims.domain;


import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.shared.domain.User;

@Entity
@Table(
    name = "item_claims",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_active_claim_per_user_per_item",
            columnNames = {"item_post_id", "claimant_user_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_post_id", nullable = false)
    private ItemPost itemPost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claimant_user_id", nullable = false)
    private User claimantUser;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "chat_thread_id")
    // private ChatThread chatThread;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClaimStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (status == null) {
            status = ClaimStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}