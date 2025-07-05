package com.learnbalochi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FileMetaDTO(
        @JsonProperty("file_name") String fileName,
        @JsonProperty("file_description") String fileDescription,
        String category,
        List<String> authors,
        @JsonProperty("is_public") boolean isPublic,
        @JsonProperty("uploader_email") String uploaderEmail,
        @JsonProperty("uploader_uid") String uploaderUid) {
}
