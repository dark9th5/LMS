package com.lmspilot.configuration.api;

import jakarta.validation.constraints.*;

import java.util.UUID;
public record BrandingRequest(@NotBlank
@Size(max=240)String systemName,@Size(max=10000)String introduction,UUID logoFileId,UUID faviconFileId,UUID backgroundFileId,@Pattern(regexp="^(unified-light|unified-dark)$")String themeKey,@Pattern(regexp="^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")String primaryColor,@Pattern(regexp="^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")String secondaryColor,@Pattern(regexp="^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")String backgroundColor,@Pattern(regexp="^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")String textColor,@Size(max=253)String customDomain){
    public BrandingRequest{
        if(themeKey==null)themeKey="unified-light";
    }

}
