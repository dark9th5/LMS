package com.lmspilot.identity.api;

import com.lmspilot.contracts.*;

import com.lmspilot.identity.domain.*;

import jakarta.validation.Valid;

import jakarta.validation.constraints.*;

import java.time.Instant;

import java.util.*;
public final class IdentityModels {
    private IdentityModels(){
    }
    public record LoginRequest(@NotBlank
    @Size(max=128) String username,@NotBlank
    @Size(max=1024) String password){
    }
    public record RefreshRequest(@NotBlank String refreshToken){
    }
    public record LogoutRequest(@NotBlank String refreshToken){
    }
    public record ChangePasswordRequest(@NotBlank String currentPassword,@NotBlank
    @Size(min=12,max=128) String newPassword){
    }
    public record TokenResponse(String accessToken,String refreshToken,String tokenType,long expiresInSeconds,UserSummary user){
        public TokenResponse(String access,String refresh,long ttl,UserSummary user){
            this(access,refresh,"Bearer",ttl,user);
        }

    }
    public record StudentDirectoryItem(UUID id,String code,String fullName,String email){
    }
    public record UserSummary(UUID id,String code,String username,String fullName,String email,UUID organizationUnitId,AccountStatus status,AccountType accountType,boolean protectedAccount,Set<String> roles,String primaryRole,Set<String> permissions,Instant lastLoginAt,boolean mustChangePassword){
    }
    public record CreateUserRequest(@NotBlank
    @Size(max=80) String code,@NotBlank
    @Size(min=3,max=120) String username,@NotBlank
    @Size(min=12,max=128) String password,@NotBlank
    @Size(max=180) String fullName,@Email String email,UUID organizationUnitId,@Size(min=1,max=1)
    Set<String> roleCodes,boolean mustChangePassword){
    }
    public record BulkCreateUsersRequest(@NotBlank
    @Size(max=120) String operationId,@Valid
    @Size(min=1,max=1000)
    List<CreateUserRequest> users){
    }
    public record BulkCreateUsersResponse(String operationId,List<UserSummary> created){
    }
    public record UpdateUserRequest(@NotBlank
    @Size(max=180) String fullName,@Email String email,UUID organizationUnitId,@Size(min=1,max=1)
    Set<String> roleCodes,AccountStatus status){
    }
    public record ResetPasswordRequest(@NotBlank
    @Size(min=12,max=128) String newPassword,boolean forceChangeOnNextLogin){
    }
    public record SessionView(UUID id,Instant issuedAt,Instant expiresAt,Instant revokedAt,String revokedReason,Instant lastUsedAt,String userAgent,String ipAddress,boolean current){
    }
    public record RoleRequest(@NotBlank
    @Size(max=80) String code,@NotBlank
    @Size(max=160) String name,Set<String> permissions){
    }
    public record RoleResponse(UUID id,String code,String name,Set<String> permissions,boolean systemRole){
    }
    public record GrantInput(String roleCode,String permissionCode,ScopeType scopeType,UUID scopeId,GrantEffect effect,Instant validFrom,Instant validUntil){
        public boolean hasExactlyOneSubject(){
            return (roleCode==null)^(permissionCode==null);
        }
        public boolean validScope(){
            return scopeType==ScopeType.SYSTEM?scopeId==null:scopeId!=null;
        }

    }
    public record BulkGrantRequest(@NotBlank
    @Size(max=120) String operationId,@Size(min=1,max=1000)
    Set<UUID> userIds,@Valid
    @Size(min=1,max=100)
    List<GrantInput> grants){
    }
    public record GrantResponse(UUID id,PrincipalType principalType,UUID principalId,String permissionCode,ScopeType scopeType,UUID scopeId,GrantEffect effect,Instant validFrom,Instant validUntil){
    }
    public record RoleAssignmentResponse(UUID id,UUID userId,String roleCode,ScopeType scopeType,UUID scopeId,GrantEffect effect,Instant validFrom,Instant validUntil){
    }
    public record BulkGrantResponse(String operationId,List<GrantResponse> permissionGrants,List<RoleAssignmentResponse> roleAssignments,int duplicateAssignments){
    }
    public record EffectivePermissionResponse(UUID userId,ScopeType scopeType,UUID scopeId,Set<String> allowed,Set<String> denied){
    }
    public record RevokeGrantsRequest(@NotBlank
    @Size(max=120) String operationId,@Size(max=5000)
    Set<UUID> grantIds,@Size(max=5000)
    Set<UUID> roleAssignmentIds){
        public RevokeGrantsRequest{
            grantIds=grantIds==null?Set.of():grantIds;
            roleAssignmentIds=roleAssignmentIds==null?Set.of():roleAssignmentIds;
        }
        public boolean hasTarget(){
            return !grantIds.isEmpty()||!roleAssignmentIds.isEmpty();
        }

    }
    public record RevokeGrantsResponse(long permissionGrantsDeleted,long roleAssignmentsDeleted){
    }
    public record PageResponse<T>(List<T> items,int page,int size,long totalElements,int totalPages){
    }
    public record UserImportMappingRequest(String codeColumn,String usernameColumn,String fullNameColumn,String emailColumn,String organizationUnitIdColumn,String roleCodesColumn,String passwordColumn,String statusColumn,Set<String> defaultRoleCodes,String defaultPassword,UserImportMode mode,UserImportFailurePolicy failurePolicy,boolean updatePasswordOnUpsert){
        public UserImportMappingRequest{
            codeColumn=blank(codeColumn,"code");
            usernameColumn=blank(usernameColumn,"username");
            fullNameColumn=blank(fullNameColumn,"fullName");
            defaultRoleCodes=defaultRoleCodes==null||defaultRoleCodes.isEmpty()?Set.of("STUDENT"):defaultRoleCodes;
            mode=mode==null?UserImportMode.CREATE_ONLY:mode;
            failurePolicy=failurePolicy==null?UserImportFailurePolicy.PARTIAL:failurePolicy;
        }
        private static String blank(String v,String d){
            return v==null||v.isBlank()?d:v;
        }

    }
    public record UserImportDetectedMapping(String codeColumn,String usernameColumn,String fullNameColumn,String emailColumn,String organizationUnitIdColumn,String roleCodesColumn,String passwordColumn,String statusColumn){
    }
    public record UserImportInspectionResponse(String fileName,List<String> headers,List<Map<String,String>> samples,UserImportDetectedMapping detectedMapping){
    }
    public record UserImportRowPreview(int rowNumber,String code,String username,String fullName,String email,UUID organizationUnitId,Set<String> roleCodes,AccountStatus status,UserImportAction action,boolean valid,List<String> errors){
    }
    public record UserImportPreviewResponse(String fileName,List<String> headers,int totalRows,int validRows,int invalidRows,int creates,int updates,List<UserImportRowPreview> rows){
    }
    public record UserImportRowResult(int rowNumber,UUID userId,String code,String username,UserImportAction action,boolean success,List<String> errors){
    }
    public record UserImportCommitResponse(String operationId,String fileName,int totalRows,int created,int updated,int skipped,int failed,boolean committed,List<UserImportRowResult> results){
    }
    public record BulkGrantPreviewRequest(@Size(min=1,max=1000)
    Set<UUID> userIds,@Valid
    @Size(min=1,max=100)
    List<GrantInput> grants){
    }
    public record UserGrantPreview(UUID userId,String fullName,Set<String> addedPermissions,Set<String> alreadyAllowed,Set<String> deniedPermissions,Set<String> excludedByScope){
    }
    public record BulkGrantPreviewResponse(int affectedUsers,int assignmentsToCreate,int duplicateAssignments,Set<String> criticalPermissions,List<UserGrantPreview> users){
    }
    public record PermissionSourceResponse(String permissionCode,String sourceType,UUID sourceId,String sourceLabel,GrantEffect effect,ScopeType scopeType,UUID scopeId,Instant validFrom,Instant validUntil,boolean active,boolean applicable){
    }
    public record AuthorizationExplanationResponse(EffectivePermissionResponse effective,List<PermissionSourceResponse> sources){
    }
    public record PermissionCatalogResponse(List<String> permissions,Map<String,List<String>> groups,List<PermissionDefinition> definitions,List<AccessProfileDefinition> profiles,Map<String,Set<String>> defaultRoles){
    }
    public record InternalAuthorizationCheckResponse(boolean allowed){
    }
    public record InternalScopeIdsResponse(Set<UUID> scopeIds){
    }
    public record InternalUserContactResponse(UUID userId,String username,String fullName,String email,boolean active){
    }

}
