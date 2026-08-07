package com.lmspilot.organization.api;

import java.util.*;
public record ValidateOrganizationUnitsRequest(Set<UUID> ids){
    public ValidateOrganizationUnitsRequest{
        ids=ids==null?Set.of():Set.copyOf(ids);
    }

}
