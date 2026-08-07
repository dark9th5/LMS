package com.lmspilot.competency.api;

import java.util.*;
public record AssignProfileRequest(Set<UUID> userIds,UUID profileId){
    public AssignProfileRequest{
        userIds=userIds==null?Set.of():Set.copyOf(userIds);
    }

}
