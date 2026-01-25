package lost_and_found.app.items.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "item_post_tags")
@Getter
@Setter
public class ItemPostTag {

    @EmbeddedId
    private ItemPostTagId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("itemPostId")
    @JoinColumn(name = "item_post_id", nullable = false)
    private ItemPost itemPost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_source", nullable = false, length = 20)
    private TagSource tagSource;
}