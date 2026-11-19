package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.domain.PostType;
import lost_and_found.app.items.dto.response.ItemPostResponseDTO;
import lost_and_found.app.items.service.ItemMapper;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.domain.UserSettings;

/** The Private Profile toggle hides the poster's name from everyone except the poster. */
class ItemMapperPrivacyTest {

    private final ItemMapper mapper = new ItemMapper();

    private User poster(boolean privateProfile) {
        User user = User.builder().id(1L).username("alice").email("alice@test.com").enabled(true).build();
        UserSettings settings = UserSettings.builder()
            .userId(1L).user(user).privateProfile(privateProfile).build();
        user.setSettings(settings);
        return user;
    }

    private ItemPost itemFrom(User user) {
        ItemPost item = new ItemPost();
        item.setId(10L);
        item.setUser(user);
        item.setTitle("Keys");
        item.setDescription("Ring of three");
        item.setCategory("Keys");
        item.setPostType(PostType.LOST);
        item.setStatus(ItemPostStatus.OPEN);
        return item;
    }

    @Test
    void hidesPosterNameFromOtherViewersWhenPrivate() {
        ItemPostResponseDTO dto = mapper.toResponse(itemFrom(poster(true)), 2L);

        assertNull(dto.posterUsername(), "a private poster's name must not leak to another viewer");
        assertEquals(1L, dto.posterId(), "the poster id stays so chat/claims still route");
    }

    @Test
    void anonymousViewerAlsoSeesNoNameWhenPrivate() {
        ItemPostResponseDTO dto = mapper.toResponse(itemFrom(poster(true)));

        assertNull(dto.posterUsername());
    }

    @Test
    void privatePosterStillSeesTheirOwnName() {
        ItemPostResponseDTO dto = mapper.toResponse(itemFrom(poster(true)), 1L);

        assertEquals("alice", dto.posterUsername());
    }

    @Test
    void publicProfileShowsNameToEveryone() {
        ItemPostResponseDTO dto = mapper.toResponse(itemFrom(poster(false)), 2L);

        assertEquals("alice", dto.posterUsername());
    }
}
