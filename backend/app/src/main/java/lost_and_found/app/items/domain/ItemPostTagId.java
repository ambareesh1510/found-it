package lost_and_found.app.items.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ItemPostTagId implements Serializable {

    @Column(name = "item_post_id")
    private Long itemPostId;

    @Column(name = "tag_id")
    private Long tagId;

    public ItemPostTagId(Long itemPostId, Long tagId) {
        this.itemPostId = itemPostId;
        this.tagId = tagId;
    }
}
