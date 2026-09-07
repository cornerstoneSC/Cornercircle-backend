package com.cornercircle.backend.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cornercircle.backend.media.dto.ImageUploadResponse;
import com.cornercircle.backend.media.exception.MediaUploadException;
import com.cornercircle.backend.media.validation.ImageValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final ImageValidator imageValidator;

    public CloudinaryService(Cloudinary cloudinary, ImageValidator imageValidator) {
        this.cloudinary = cloudinary;
        this.imageValidator = imageValidator;
    }

    public ImageUploadResponse uploadImage(MultipartFile file, String folder) {
        imageValidator.validate(file);

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", folder, "resource_type", "image")
            );

            String url = uploadResult.get("secure_url").toString();
            String publicId = uploadResult.get("public_id").toString();

            return new ImageUploadResponse(url, publicId);

        } catch (Exception exception) {
            throw new MediaUploadException("Failed to upload image to Cloudinary", exception);
        }
    }

    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException exception) {
            throw new MediaUploadException("Failed to delete image from Cloudinary", exception);
        }
    }
}
