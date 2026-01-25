package lost_and_found.app.items.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.locationtech.jts.geom.Point;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lost_and_found.app.claims.domain.ItemClaim;
import lost_and_found.app.shared.domain.User;

@Entity
@Table(name = "item_posts")
@Getter
@Setter
public class ItemPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // assumes existing User entity mapped to users table
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 20)
    private PostType postType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ItemPostStatus status;

    /** Optional: a photo is encouraged but not required (ui-spec, decided by the client). */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "image_url2", columnDefinition = "TEXT")
    private String imageUrl2;

    /** Reward offered by the loser, in cents. LOST posts only; null means no reward. */
    @Column(name = "reward_cents")
    private Integer rewardCents;

    /**
     * The counterparty the poster proposed resolving this item with. Null unless a resolution is in
     * progress or complete. Resolution requires both the poster and this user to confirm.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_with_user_id")
    private User resolvedWithUser;

    /** True once the poster has proposed the resolution. */
    @Column(name = "poster_confirmed_resolution", nullable = false)
    private boolean posterConfirmedResolution;

    /** True once {@link #resolvedWithUser} has confirmed the resolution. */
    @Column(name = "counterparty_confirmed_resolution", nullable = false)
    private boolean counterpartyConfirmedResolution;

    @Column(name = "location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "location_text", columnDefinition = "TEXT")
    private String locationText;

    @Column(name = "detail_location", columnDefinition = "TEXT")
    private String detailLocation;

    @Column(name = "event_time")
    private Instant eventTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "itemPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ItemPostTag> itemPostTags = new HashSet<>();

    @OneToMany(mappedBy = "itemPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ItemClaim> claims = new HashSet<>();

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.postType == null) {
            this.postType = PostType.LOST;
        }
        if (this.status == null) {
            this.status = ItemPostStatus.OPEN;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
