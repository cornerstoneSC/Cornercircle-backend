package com.cornercircle.backend.homepage.controller;

import com.cornercircle.backend.homepage.service.HomepageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class HomepageControllerTest {
    private HomepageService service;
    private HomepageController controller;
    private MockMultipartFile image;

    @BeforeEach
    void setUp() {
        service = mock(HomepageService.class);
        controller = new HomepageController(service);
        ReflectionTestUtils.setField(controller, "contentApiSecret", "a".repeat(32));
        image = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1});
    }

    @Test
    void heroUploadRequiresAdminSecret() {
        assertThrows(ResponseStatusException.class, () -> controller.uploadHeroImage(null, image));
        verifyNoInteractions(service);
    }

    @Test
    void aboutUploadRequiresAdminSecret() {
        assertThrows(ResponseStatusException.class, () -> controller.uploadAboutImage("wrong", image));
        verifyNoInteractions(service);
    }

    @Test
    void beliefsUploadRequiresAdminSecret() {
        assertThrows(ResponseStatusException.class, () -> controller.uploadBeliefsImage(null, image));
        verifyNoInteractions(service);
    }
}
