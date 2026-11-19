package lost_and_found.app.shared.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Guards the found-match email's content. The alert must carry NO found-item detail (the redesign
 * keeps that behind a declared search), only the recipient's own report title and the app link.
 */
class EmailServiceContentTest {

    private final EmailService emailService = new EmailService("http://localhost:5173/", "", "GT <a@b.com>");

    @Test
    void foundMatchEmailLinksToTheSearchFlowAndNamesTheOwnReport() {
        String html = emailService.buildFoundMatchHtml("Blue Nalgene");

        assertTrue(html.contains("http://localhost:5173/lost"), "must link into the lost-search flow");
        assertTrue(html.contains("Blue Nalgene"), "may reference the recipient's own report title");
        assertTrue(html.toLowerCase().contains("don't share found-item details")
            || html.toLowerCase().contains("do not share found-item details")
            || html.contains("don't share"), "should state that found-item details are not shared");
    }

    @Test
    void foundMatchEmailEscapesTheTitleSoItCannotInjectHtml() {
        String html = emailService.buildFoundMatchHtml("<script>alert(1)</script>");

        assertFalse(html.contains("<script>"), "user-supplied title must be HTML-escaped");
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void foundMatchEmailHandlesAMissingTitle() {
        String html = emailService.buildFoundMatchHtml("   ");

        assertTrue(html.contains("one of your lost reports"));
    }
}
