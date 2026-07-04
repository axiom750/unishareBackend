package com.unishare.service.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final Cloudinary cloudinary;

    public Map<String, String> uploadImage(
            MultipartFile file,
            String folder,
            String publicId,
            boolean overwrite) {

        try {

            if (file.isEmpty())
                throw new IllegalArgumentException("File cannot be empty");

            String contentType = file.getContentType();

            if (contentType == null || !contentType.startsWith("image/"))
                throw new IllegalArgumentException("Only image files allowed");

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of(
                            "folder", folder,
                            "public_id", publicId,
                            "overwrite", overwrite,
                            "invalidate", true,
                            "resource_type", "image",
                            "transformation", new Transformation()
                                    .width(1200)
                                    .crop("limit")
                                    .quality("auto")
                                    .fetchFormat("auto")
                    )
            );

            return Map.of(
                    "url", uploadResult.get("secure_url").toString(),
                    "publicId", uploadResult.get("public_id").toString()
            );

        } catch (IOException e) {
            throw new RuntimeException("Cloudinary upload failed");
        }
    }

    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, Map.of("invalidate", true));
        } catch (IOException e) {
            throw new RuntimeException("Cloudinary delete failed");
        }
    }
}