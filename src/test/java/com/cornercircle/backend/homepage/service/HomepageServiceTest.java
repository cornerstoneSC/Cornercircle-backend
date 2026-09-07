package com.cornercircle.backend.homepage.service;

import com.cornercircle.backend.homepage.model.HomepageContent;
import com.cornercircle.backend.homepage.repository.HomepageRepository;
import com.cornercircle.backend.media.dto.ImageUploadResponse;
import com.cornercircle.backend.media.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HomepageServiceTest {
    private final HomepageRepository repository = mock(HomepageRepository.class);
    private final CloudinaryService cloud = mock(CloudinaryService.class);
    private final HomepageService service = new HomepageService(repository, cloud);

    @Test
    void savesAndReloadsBeliefsPhotoWithoutChangingHeroOrAbout() {
        var content = new HomepageContent();
        content.setHeroImageUrl("hero");
        content.setAboutImageUrl("about");
        when(repository.findById(1L)).thenReturn(Optional.of(content));
        when(repository.save(content)).thenReturn(content);
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1});
        when(cloud.uploadImage(file, "homepage"))
                .thenReturn(new ImageUploadResponse("https://example.com/beliefs.png", "beliefs-id"));
        var result = service.uploadBeliefsImage(file);
        assertEquals("https://example.com/beliefs.png", result.getBeliefsImageUrl());
        assertEquals("beliefs-id", result.getBeliefsImagePublicId());
        assertEquals("hero", result.getHeroImageUrl());
        assertEquals("about", result.getAboutImageUrl());
        assertEquals(result.getBeliefsImageUrl(), service.getHomepage().getBeliefsImageUrl());
        verify(repository).save(content);
    }

    @Test
    void failedUploadKeepsExistingPhotoAndDoesNotSave() {
        var content = new HomepageContent();
        content.setBeliefsImageUrl("previous");
        when(repository.findById(1L)).thenReturn(Optional.of(content));
        var file = new MockMultipartFile("file", new byte[]{1});
        when(cloud.uploadImage(file, "homepage")).thenThrow(new IllegalStateException("failed"));
        assertThrows(IllegalStateException.class, () -> service.uploadBeliefsImage(file));
        assertEquals("previous", content.getBeliefsImageUrl());
        verify(repository, never()).save(any());
    }

    @Test
    void existingHomepageWithoutBeliefsPhotoUsesEmptyField() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertNull(service.getHomepage().getBeliefsImageUrl());
    }
}
