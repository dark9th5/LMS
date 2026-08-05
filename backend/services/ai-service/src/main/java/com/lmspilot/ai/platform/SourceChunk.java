package com.lmspilot.ai.platform;

import java.util.UUID;

public record SourceChunk(UUID documentVersionId, Integer page, String section, String text) {}
