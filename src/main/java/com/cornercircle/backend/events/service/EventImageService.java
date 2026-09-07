package com.cornercircle.backend.events.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cornercircle.backend.events.dto.EventImageUploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@Service
public class EventImageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Cloudinary cloudinary;

    public EventImageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public EventImageUploadResponse upload(
            MultipartFile file
    ) {
        validateImage(file);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result =
                    cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "folder",
                                    "cornerstone/events",
                                    "resource_type",
                                    "image",
                                    "use_filename",
                                    true,
                                    "unique_filename",
                                    true
                            )
                    );

            String secureUrl =
                    String.valueOf(
                            result.get("secure_url")
                    );

            String publicId =
                    String.valueOf(
                            result.get("public_id")
                    );

            return new EventImageUploadResponse(
                    secureUrl,
                    publicId
            );

        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to upload event image",
                    exception
            );
        }
    }

    private void validateImage(
            MultipartFile file
    ) {
        if (
                file == null ||
                        file.isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image file is required"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image must be 10 MB or smaller"
            );
        }

        String contentType =
                file.getContentType();

        if (
                contentType == null ||
                        !(contentType.equals("image/png") || contentType.equals("image/jpeg") || contentType.equals("image/webp"))
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PNG, JPEG, or WebP images are allowed"
            );
        }
    }
}
