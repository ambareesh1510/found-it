package lost_and_found.app.items.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostTag;
import lost_and_found.app.items.domain.ItemPostTagId;
import lost_and_found.app.items.domain.Tag;
import lost_and_found.app.items.domain.TagSource;
import lost_and_found.app.items.infra.ItemPostRepository;
import lost_and_found.app.items.infra.ItemPostTagRepository;
import lost_and_found.app.items.infra.TagRepository;

@Service
public class VisionTaggingService {

    private static final Logger log = LoggerFactory.getLogger(VisionTaggingService.class);
    private static final double MIN_CONFIDENCE = 0.1;
    private static final int MAX_TAGS = 3;

    private final WebClient webClient;
    private final ItemPostRepository itemPostRepository;
    private final TagRepository tagRepository;
    private final ItemPostTagRepository itemPostTagRepository;
    private final boolean enabled;
    private final String apiToken;

    public VisionTaggingService(
            @Value("${vision.huggingface.api-url}") String apiUrl,
            @Value("${vision.huggingface.model}") String model,
            @Value("${vision.huggingface.api-token:}") String apiToken,
            @Value("${vision.huggingface.enabled:true}") boolean enabled,
            ItemPostRepository itemPostRepository,
            TagRepository tagRepository,
            ItemPostTagRepository itemPostTagRepository
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl + model)
                .build();
        this.itemPostRepository = itemPostRepository;
        this.tagRepository = tagRepository;
        this.itemPostTagRepository = itemPostTagRepository;
        this.enabled = enabled;
        this.apiToken = apiToken;
    }

    @Async
    @Transactional
    public void tagItemAsync(Long itemPostId) {
        try {
            if (!enabled) {
                log.debug("Vision tagging is disabled, skipping item {}", itemPostId);
                return;
            }
            if (apiToken == null || apiToken.isBlank()) {
                log.warn("No Hugging Face API token configured, skipping vision tagging");
                return;
            }

            ItemPost item = itemPostRepository.findById(itemPostId).orElse(null);
            if (item == null) {
                log.warn("Item {} not found for vision tagging", itemPostId);
                return;
            }

            int tagged = 0;
            tagged += tagFromImage(item, item.getImageUrl());
            tagged += tagFromImage(item, item.getImageUrl2());
            log.info("Vision tagged item {} with {} tags", itemPostId, tagged);
        } catch (Exception e) {
            log.error("Vision tagging failed for item {}: {}", itemPostId, e.getMessage());
        }
    }

    /** Runs object classification + color detection on one image URL. Returns count of tags saved. */
    private int tagFromImage(ItemPost item, String imageUrl) {
        byte[] imageBytes = extractImageBytes(imageUrl);
        if (imageBytes == null) return 0;

        int saved = 0;

        // Object classification via Hugging Face
        try {
            List<Map<String, Object>> results = callHuggingFace(imageBytes);
            if (results != null) {
                int tagged = 0;
                for (Map<String, Object> result : results) {
                    if (tagged >= MAX_TAGS) break;
                    double score = ((Number) result.get("score")).doubleValue();
                    if (score < MIN_CONFIDENCE) continue;
                    String label = cleanLabel((String) result.get("label"));
                    Tag tag = findOrCreateTag(label, "vision-auto");
                    if (!itemPostTagRepository.existsByItemPostIdAndTagId(item.getId(), tag.getId())) {
                        ItemPostTag ipt = new ItemPostTag();
                        ipt.setId(new ItemPostTagId(item.getId(), tag.getId()));
                        ipt.setItemPost(item);
                        ipt.setTag(tag);
                        ipt.setTagSource(TagSource.VISION);
                        itemPostTagRepository.save(ipt);
                        tagged++;
                        saved++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("HuggingFace call failed for item {}: {}", item.getId(), e.getMessage());
        }

        // Color detection — pure pixel analysis, no API call
        String dominantColor = detectDominantColor(imageBytes);
        if (dominantColor != null) {
            Tag colorTag = findOrCreateTag(dominantColor, "color");
            if (!itemPostTagRepository.existsByItemPostIdAndTagId(item.getId(), colorTag.getId())) {
                ItemPostTag ipt = new ItemPostTag();
                ipt.setId(new ItemPostTagId(item.getId(), colorTag.getId()));
                ipt.setItemPost(item);
                ipt.setTag(colorTag);
                ipt.setTagSource(TagSource.VISION);
                itemPostTagRepository.save(ipt);
                saved++;
            }
        }

        return saved;
    }

    private byte[] extractImageBytes(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("data:image")) {
            return null;
        }
        int commaIdx = imageUrl.indexOf(',');
        if (commaIdx < 0) return null;
        String base64Data = imageUrl.substring(commaIdx + 1);
        return Base64.getDecoder().decode(base64Data);
    }

    private List<Map<String, Object>> callHuggingFace(byte[] imageBytes) {
        return webClient.post()
                .header("Authorization", "Bearer " + apiToken)
                .contentType(MediaType.IMAGE_JPEG)
                .bodyValue(imageBytes)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block();
    }

    private String cleanLabel(String label) {
        if (label == null) return "unknown";
        // HuggingFace labels can look like "backpack, knapsack" - take the first
        int commaIdx = label.indexOf(',');
        String cleaned = commaIdx > 0 ? label.substring(0, commaIdx) : label;
        return cleaned.trim().toLowerCase();
    }

    private Tag findOrCreateTag(String name, String category) {
        return tagRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Tag tag = new Tag();
                    tag.setName(name);
                    tag.setCategory(category);
                    return tagRepository.save(tag);
                });
    }

    /**
     * Detects the dominant color of an image by sampling pixels and mapping
     * the average hue/saturation/brightness to a human-readable color name.
     */
    public String detectDominantColor(byte[] imageBytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) return null;

            int width = img.getWidth();
            int height = img.getHeight();

            // Sample center 60% of the image to avoid background
            int x0 = (int) (width * 0.2);
            int y0 = (int) (height * 0.2);
            int x1 = (int) (width * 0.8);
            int y1 = (int) (height * 0.8);

            long totalR = 0, totalG = 0, totalB = 0;
            int sampleCount = 0;
            int step = Math.max(1, (x1 - x0) * (y1 - y0) / 10000); // sample ~10k pixels max

            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    if (sampleCount % step != 0) { sampleCount++; continue; }
                    int rgb = img.getRGB(x, y);
                    totalR += (rgb >> 16) & 0xFF;
                    totalG += (rgb >> 8) & 0xFF;
                    totalB += rgb & 0xFF;
                    sampleCount++;
                }
            }

            if (sampleCount == 0) return null;

            int samplesUsed = sampleCount / step;
            if (samplesUsed == 0) return null;

            int avgR = (int) (totalR / samplesUsed);
            int avgG = (int) (totalG / samplesUsed);
            int avgB = (int) (totalB / samplesUsed);

            return rgbToColorName(avgR, avgG, avgB);
        } catch (Exception e) {
            log.debug("Color detection failed: {}", e.getMessage());
            return null;
        }
    }

    private String rgbToColorName(int r, int g, int b) {
        float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360;
        float sat = hsb[1];
        float bri = hsb[2];

        // Achromatic colors
        if (bri < 0.15) return "black";
        if (bri > 0.85 && sat < 0.15) return "white";
        if (sat < 0.15) return "gray";

        // Chromatic colors by hue
        if (hue < 15)  return "red";
        if (hue < 40)  return "orange";
        if (hue < 70)  return "yellow";
        if (hue < 160) return "green";
        if (hue < 200) return "teal";
        if (hue < 250) return "blue";
        if (hue < 290) return "purple";
        if (hue < 330) return "pink";
        return "red";
    }
}
