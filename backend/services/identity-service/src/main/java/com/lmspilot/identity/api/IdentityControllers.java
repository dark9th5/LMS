package com.lmspilot.identity.api;

import com.lmspilot.contracts.*;

import com.lmspilot.identity.api.IdentityModels.*;

import com.lmspilot.identity.domain.*;

import com.lmspilot.identity.service.*;

import com.lmspilot.support.security.*;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.Valid;

import java.util.*;

import org.springframework.http.*;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
    private final AuthService auth;
    AuthController(AuthService auth){
        this.auth=auth;
    }
    @PostMapping("/login")
    TokenResponse login(@Valid
    @RequestBody LoginRequest i,HttpServletRequest r){
        return auth.login(i,r);
    }
    @PostMapping("/refresh")
    TokenResponse refresh(@Valid
    @RequestBody RefreshRequest i,HttpServletRequest r){
        return auth.refresh(i.refreshToken(),r);
    }
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid
    @RequestBody LogoutRequest i){
        auth.logout(i.refreshToken());
    }
    @GetMapping("/me")
    UserSummary me(){
        return auth.me(CurrentUser.id());
    }
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void password(@Valid
    @RequestBody ChangePasswordRequest i){
        auth.changePassword(CurrentUser.id(),i);
    }
    @GetMapping("/sessions")
    List<SessionView> sessions(){
        return auth.sessions(CurrentUser.id());
    }
    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@PathVariable UUID id){
        auth.revokeSession(CurrentUser.id(),id);
    }
    @DeleteMapping("/sessions")
    Map<String,Integer> revokeAll(){
        return Map.of("revoked",auth.revokeAllSessions(CurrentUser.id()));
    }

}
@RestController
@RequestMapping("/api/v1/users")
class UserController {
    private final UserManagementService service;
    private final UserImportService imports;
    UserController(UserManagementService s,UserImportService i){
        service=s;
        imports=i;
    }
    @GetMapping
    PageResponse<UserSummary> search(@RequestParam(required=false) String query,@RequestParam(required=false) AccountStatus status,@RequestParam(required=false) String role,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
        return service.search(query,status,role,page,size);
    }
    @GetMapping("/{id}")
    UserSummary get(@PathVariable UUID id){
        return service.get(id);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserSummary create(@Valid
    @RequestBody CreateUserRequest i){
        return service.create(i);
    }
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    BulkCreateUsersResponse bulk(@Valid
    @RequestBody BulkCreateUsersRequest i){
        return service.bulkCreate(i);
    }
    @PostMapping(value="/import/inspect",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    UserImportInspectionResponse inspect(@RequestPart("file") MultipartFile f){
        return imports.inspect(f);
    }
    @PostMapping(value="/import/preview",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    UserImportPreviewResponse preview(@RequestPart("file") MultipartFile f,@Valid
    @RequestPart("mapping") UserImportMappingRequest m){
        return imports.preview(f,m);
    }
    @PostMapping(value="/import/commit",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    UserImportCommitResponse commit(@RequestPart("file") MultipartFile f,@Valid
    @RequestPart("mapping") UserImportMappingRequest m,@RequestPart("operationId") String id){
        return imports.commit(f,m,id);
    }
    @PutMapping("/{id}")
    UserSummary update(@PathVariable UUID id,@Valid
    @RequestBody UpdateUserRequest i){
        return service.update(id,i);
    }
    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reset(@PathVariable UUID id,@Valid
    @RequestBody ResetPasswordRequest i){
        service.resetPassword(id,i);
    }

}
@RestController
@RequestMapping("/api/v1/directory")
class DirectoryController {
    private final UserManagementService service;
    DirectoryController(UserManagementService s){
        service=s;
    }
    @GetMapping("/students")
    List<StudentDirectoryItem> students(){
        return service.studentDirectory();
    }

}
@RestController
@RequestMapping("/api/v1/roles")
class RoleController {
    private final UserManagementService service;
    RoleController(UserManagementService s){
        service=s;
    }
    @GetMapping
    List<RoleResponse> list(){
        return service.listRoles();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RoleResponse create(@Valid
    @RequestBody RoleRequest i){
        return service.createRole(i);
    }
    @PutMapping("/{id}")
    RoleResponse update(@PathVariable UUID id,@Valid
    @RequestBody RoleRequest i){
        return service.updateRole(id,i);
    }

}
@RestController
@RequestMapping("/api/v1/authorization")
class AuthorizationController {
    private final AuthorizationService service;
    AuthorizationController(AuthorizationService s){
        service=s;
    }
    @PostMapping("/grants/preview")
    BulkGrantPreviewResponse preview(@Valid
    @RequestBody BulkGrantPreviewRequest i){
        return service.previewBulk(i);
    }
    @PostMapping("/grants/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    BulkGrantResponse grant(@Valid
    @RequestBody BulkGrantRequest i){
        return service.grantBulk(i);
    }
    @DeleteMapping("/grants/bulk")
    RevokeGrantsResponse revoke(@Valid
    @RequestBody RevokeGrantsRequest i){
        return service.revokeBulk(i);
    }
    @GetMapping("/effective")
    EffectivePermissionResponse effective(@RequestParam UUID userId,@RequestParam ScopeType scopeType,@RequestParam(required=false) UUID scopeId){
        return service.effective(userId,scopeType,scopeId);
    }
    @GetMapping("/explain")
    AuthorizationExplanationResponse explain(@RequestParam UUID userId,@RequestParam ScopeType scopeType,@RequestParam(required=false) UUID scopeId){
        return service.explain(userId,scopeType,scopeId);
    }
    @GetMapping("/catalog")
    PermissionCatalogResponse catalog(){
        var defs=PermissionCatalog.all();
        Map<String,List<String>> groups=new TreeMap<>();
        for(var d:defs)groups.computeIfAbsent(d.group(),x->new ArrayList<>()).add(d.code());
        Map<String,Set<String>> defaults=new LinkedHashMap<>();
        for(var p:DefaultAccessProfiles.PROFILES)defaults.put(p.code(),p.permissions());
        return new PermissionCatalogResponse(defs.stream().map(PermissionDefinition::code).toList(),groups,defs,DefaultAccessProfiles.PROFILES,defaults);
    }
    @GetMapping("/users/{id}/assignments")
    List<RoleAssignmentResponse> assignments(@PathVariable UUID id){
        return service.assignments(id);
    }

}
@RestController
@RequestMapping("/internal/v1/users")
class InternalUserController {
    private final UserManagementService service;
    private final InternalTokenAuthorizer internal;
    InternalUserController(UserManagementService s,InternalTokenAuthorizer i){
        service=s;
        internal=i;
    }
    @GetMapping("/{id}/contact")
    InternalUserContactResponse contact(@PathVariable UUID id,@RequestHeader(value="X-Service-Token",required=false) String token){
        internal.require(token);
        UserSummary u=service.get(id);
        return new InternalUserContactResponse(u.id(),u.username(),u.fullName(),u.email(),u.status()==AccountStatus.ACTIVE);
    }

}
@RestController
@RequestMapping("/api/v1/users/{userId}/sessions")
class UserSessionAdminController {
    private final AuthService auth;
    UserSessionAdminController(AuthService a){
        auth=a;
    }
    @GetMapping
    List<SessionView> list(@PathVariable UUID userId){
        return auth.sessions(userId);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@PathVariable UUID userId,@PathVariable UUID id){
        auth.revokeSession(userId,id,"ADMIN_REVOKED");
    }
    @DeleteMapping
    Map<String,Integer> all(@PathVariable UUID userId){
        return Map.of("revoked",auth.revokeAllSessions(userId,"ADMIN_REVOKED_ALL"));
    }

}
@RestController
@RequestMapping("/internal/v1/authorization")
class InternalAuthorizationController {
    private final AuthorizationService service;
    private final InternalTokenAuthorizer internal;
    InternalAuthorizationController(AuthorizationService s,InternalTokenAuthorizer i){
        service=s;
        internal=i;
    }
    @GetMapping("/check")
    InternalAuthorizationCheckResponse check(@RequestParam UUID userId,@RequestParam String permission,@RequestParam ScopeType scopeType,@RequestParam(required=false) UUID scopeId,@RequestHeader(value="X-Service-Token",required=false) String token){
        internal.require(token);
        return new InternalAuthorizationCheckResponse(service.check(userId,permission,scopeType,scopeId));
    }
    @GetMapping("/scope-ids")
    InternalScopeIdsResponse ids(@RequestParam UUID userId,@RequestParam String permission,@RequestParam ScopeType scopeType,@RequestHeader(value="X-Service-Token",required=false) String token){
        internal.require(token);
        return new InternalScopeIdsResponse(service.scopeIds(userId,permission,scopeType));
    }

}
