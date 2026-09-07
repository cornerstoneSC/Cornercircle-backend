package com.cornercircle.backend.media.validation;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.cornercircle.backend.media.exception.InvalidMediaException;
import java.util.Set;

@Component
public class ImageValidator {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = 
    Set.of(
        "image/jpeg", 
        "image/png", 
        "image/webp"
    );

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()){
            throw new InvalidMediaException(
                "Image file cannot be empty."
            );
        }
        if(file.getSize() > MAX_FILE_SIZE){
            throw new InvalidMediaException(
                "Image file size exceeds maximum allowed size of 10MB."
            );
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)){
            throw new InvalidMediaException(
                "Only JPEG, PNG and WebP images are allowed."
            );
        }
    }  

}