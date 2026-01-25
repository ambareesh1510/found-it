package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import lost_and_found.app.items.domain.PostType;
import lost_and_found.app.items.dto.request.CreateItemDTO;

class CreateItemDTOValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static CreateItemDTO dto(String imageUrl, PostType postType, Integer rewardCents) {
        return new CreateItemDTO(
            "Keys", null, "Keys", imageUrl, null, 33.77, -84.39,
            "Clough", null, Instant.parse("2026-03-14T10:00:00Z"), postType, rewardCents
        );
    }

    private static Set<String> messages(CreateItemDTO dto) {
        return VALIDATOR.validate(dto).stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toSet());
    }

    @Test
    void photoIsOptional() {
        assertTrue(messages(dto(null, PostType.LOST, null)).isEmpty());
        assertTrue(messages(dto("", PostType.FOUND, null)).isEmpty());
    }

    @Test
    void photoMustBeAnHttpLinkOrImageDataUrlWhenGiven() {
        assertTrue(messages(dto("https://example.com/a.jpg", PostType.LOST, null)).isEmpty());
        assertTrue(messages(dto("data:image/png;base64,iVBORw0KGgo=", PostType.LOST, null)).isEmpty());
        assertTrue(messages(dto("ftp://example.com/a.jpg", PostType.LOST, null))
            .contains("Photo must be an http(s) link or an image file."));
        assertTrue(messages(dto("javascript:alert(1)", PostType.LOST, null))
            .contains("Photo must be an http(s) link or an image file."));
    }

    @Test
    void rewardIsAllowedOnLostPosts() {
        assertTrue(messages(dto(null, PostType.LOST, 5000)).isEmpty());
    }

    @Test
    void omittedPostTypeCountsAsLostSoRewardIsAllowed() {
        assertTrue(messages(dto(null, null, 5000)).isEmpty());
    }

    @Test
    void rewardIsRejectedOnFoundPosts() {
        assertTrue(messages(dto(null, PostType.FOUND, 5000))
            .contains("Reward applies only to lost reports."));
    }

    @Test
    void rewardMustBeWithinRange() {
        assertTrue(messages(dto(null, PostType.LOST, -1)).contains("Reward cannot be negative."));
        assertTrue(messages(dto(null, PostType.LOST, 10_000_001))
            .contains("Reward cannot be more than $100,000."));
        assertTrue(messages(dto(null, PostType.LOST, 10_000_000)).isEmpty());
    }
}
