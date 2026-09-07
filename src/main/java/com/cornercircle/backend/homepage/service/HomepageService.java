package com.cornercircle.backend.homepage.service;

import com.cornercircle.backend.homepage.dto.HomepageResponse;
import com.cornercircle.backend.homepage.dto.HomepageContentRequest;
import com.cornercircle.backend.homepage.model.HomepageContent;
import com.cornercircle.backend.homepage.repository.HomepageRepository;
import com.cornercircle.backend.media.dto.ImageUploadResponse;
import com.cornercircle.backend.media.service.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HomepageService {

    private static final Long HOMEPAGE_ID = 1L;

    private final HomepageRepository homepageRepository;
    private final CloudinaryService cloudinaryService;

    public HomepageService(
            HomepageRepository homepageRepository,
            CloudinaryService cloudinaryService
    ) {
        this.homepageRepository = homepageRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public HomepageResponse uploadHeroImage(MultipartFile file) {

        HomepageContent homepage =
                homepageRepository.findById(HOMEPAGE_ID)
                        .orElseGet(HomepageContent::new);

        ImageUploadResponse uploadResult =
                cloudinaryService.uploadImage(file, "homepage");

        String imageUrl = uploadResult.url();
        String publicId = uploadResult.publicId();

        homepage.setHeroImageUrl(imageUrl);
        homepage.setHeroImagePublicId(publicId);

        HomepageContent saved =
                homepageRepository.save(homepage);

        return new HomepageResponse(
                saved.getId(),
                saved.getHeroImageUrl(),
                saved.getHeroImagePublicId(),
                saved.getAboutImageUrl(),
                saved.getAboutImagePublicId(),
                saved.getBeliefsImageUrl(),
                saved.getBeliefsImagePublicId()
        );
    }

    public HomepageResponse uploadAboutImage(MultipartFile file) {

        HomepageContent homepage =
                homepageRepository.findById(HOMEPAGE_ID)
                        .orElseGet(HomepageContent::new);

        ImageUploadResponse uploadResult =
                cloudinaryService.uploadImage(file, "homepage");

        String imageUrl = uploadResult.url();
        String publicId = uploadResult.publicId();

        homepage.setAboutImageUrl(imageUrl);
        homepage.setAboutImagePublicId(publicId);

        HomepageContent saved =
                homepageRepository.save(homepage);

        return new HomepageResponse(
                saved.getId(),
                saved.getHeroImageUrl(),
                saved.getHeroImagePublicId(),
                saved.getAboutImageUrl(),
                saved.getAboutImagePublicId(),
                saved.getBeliefsImageUrl(),
                saved.getBeliefsImagePublicId()
        );
    }

    public HomepageResponse uploadBeliefsImage(MultipartFile file) {
        HomepageContent homepage = homepageRepository.findById(HOMEPAGE_ID)
                .orElseGet(HomepageContent::new);
        ImageUploadResponse uploadResult = cloudinaryService.uploadImage(file, "homepage");
        homepage.setBeliefsImageUrl(uploadResult.url());
        homepage.setBeliefsImagePublicId(uploadResult.publicId());
        HomepageContent saved = homepageRepository.save(homepage);
        return new HomepageResponse(saved.getId(), saved.getHeroImageUrl(),
                saved.getHeroImagePublicId(), saved.getAboutImageUrl(),
                saved.getAboutImagePublicId(), saved.getBeliefsImageUrl(),
                saved.getBeliefsImagePublicId());
    }

    public HomepageResponse uploadFounderImage(MultipartFile file) {
        HomepageContent homepage = homepageRepository.findById(HOMEPAGE_ID).orElseGet(HomepageContent::new);
        ImageUploadResponse uploadResult = cloudinaryService.uploadImage(file, "homepage-founder");
        homepage.setFounderImageUrl(uploadResult.url());
        homepage.setFounderImagePublicId(uploadResult.publicId());
        HomepageContent saved = homepageRepository.save(homepage);
        return toResponse(saved);
    }

    public HomepageResponse getHomepage() {
        HomepageContent homepage =
                homepageRepository.findById(HOMEPAGE_ID)
                        .orElseGet(HomepageContent::new);

        HomepageResponse response = toResponse(homepage);
        response.setContentJson(homepage.getContentJson());
        return response;
    }

    private HomepageResponse toResponse(HomepageContent homepage) {
        HomepageResponse response = new HomepageResponse(homepage.getId(), homepage.getHeroImageUrl(),
                homepage.getHeroImagePublicId(), homepage.getAboutImageUrl(), homepage.getAboutImagePublicId(),
                homepage.getBeliefsImageUrl(), homepage.getBeliefsImagePublicId());
        response.setFounderImageUrl(homepage.getFounderImageUrl());
        response.setFounderImagePublicId(homepage.getFounderImagePublicId());
        response.setContentJson(homepage.getContentJson());
        return response;
    }

    @Transactional
    public HomepageResponse updateContent(HomepageContentRequest request) {
        HomepageContent homepage = homepageRepository.findById(HOMEPAGE_ID).orElseGet(HomepageContent::new);
        homepage.setContentJson(request.contentJson());
        homepageRepository.save(homepage);
        return getHomepage();
    }


}
