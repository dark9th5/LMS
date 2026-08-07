package com.lmspilot.competency.api;

import com.lmspilot.competency.domain.*;

import com.lmspilot.contracts.EventTypes;

import com.lmspilot.contracts.Permissions;

import com.lmspilot.support.api.ApiException;

import com.lmspilot.support.events.DomainEventPublisher;

import com.lmspilot.support.security.CurrentUser;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.*;

import java.util.stream.Collectors;
@Service
public class CompetencyService {
    private final CompetencyRepository competencies;
    private final CompetencyProfileRepository profiles;
    private final CompetencyProfileRequirementRepository requirements;
    private final UserCompetencyProfileRepository assignments;
    private final UserCompetencyAssessmentRepository assessments;
    private final CourseCompetencyMapRepository courseMaps;
    private final DomainEventPublisher events;
    public CompetencyService(CompetencyRepository a,CompetencyProfileRepository b,CompetencyProfileRequirementRepository c,UserCompetencyProfileRepository d,UserCompetencyAssessmentRepository e,CourseCompetencyMapRepository f,DomainEventPublisher g){
        competencies=a;
        profiles=b;
        requirements=c;
        assignments=d;
        assessments=e;
        courseMaps=f;
        events=g;
    }
    @Transactional(readOnly=true)
    public List<CompetencyView> listCompetencies(boolean includeInactive){
        return (includeInactive?competencies.findAll():competencies.findAllByStatusOrderByCategoryAscNameAsc(CompetencyStatus.ACTIVE)).stream().map(this::view).toList();
    }
    @Transactional
    public CompetencyView createCompetency(CompetencyRequest i){
        if(competencies.existsByCodeIgnoreCase(i.code().trim()))throw conflict("Mã năng lực đã tồn tại");
        return view(competencies.save(new CompetencyEntity(i.code().trim().toUpperCase(),i.name().trim(),trim(i.description()),trim(i.category()),i.maxLevel(),i.active()?CompetencyStatus.ACTIVE:CompetencyStatus.INACTIVE)));
    }
    @Transactional
    public CompetencyView updateCompetency(UUID id,CompetencyRequest i){
        var e=competencies.findById(id).orElseThrow(()->notFound("COMPETENCY_NOT_FOUND","Không tìm thấy năng lực"));
        e.setName(i.name().trim());
        e.setDescription(trim(i.description()));
        e.setCategory(trim(i.category()));
        e.setMaxLevel(i.maxLevel());
        e.setStatus(i.active()?CompetencyStatus.ACTIVE:CompetencyStatus.INACTIVE);
        e.setUpdatedAt(Instant.now());
        return view(e);
    }
    @Transactional(readOnly=true)
    public List<ProfileView> listProfiles(){
        return profiles.findAllByActiveTrueOrderByNameAsc().stream().map(this::profileView).toList();
    }
    @Transactional
    public ProfileView createProfile(ProfileRequest i){
        if(profiles.existsByCodeIgnoreCase(i.code().trim()))throw conflict("Mã khung năng lực đã tồn tại");
        var p=profiles.save(new CompetencyProfileEntity(i.code().trim().toUpperCase(),i.name().trim(),trim(i.description()),i.organizationUnitId(),upper(i.roleCode()),i.active()));
        replaceRequirements(p,i.requirements());
        return profileView(p);
    }
    @Transactional
    public ProfileView updateProfile(UUID id,ProfileRequest i){
        var p=profiles.findById(id).orElseThrow(()->notFound("PROFILE_NOT_FOUND","Không tìm thấy khung năng lực"));
        p.setName(i.name().trim());
        p.setDescription(trim(i.description()));
        p.setOrganizationUnitId(i.organizationUnitId());
        p.setRoleCode(upper(i.roleCode()));
        p.setActive(i.active());
        p.setUpdatedAt(Instant.now());
        replaceRequirements(p,i.requirements());
        return profileView(p);
    }
    @Transactional
    public int assignProfile(AssignProfileRequest i){
        var p=profiles.findById(i.profileId()).orElseThrow(()->notFound("PROFILE_NOT_FOUND","Không tìm thấy khung năng lực"));
        int n=0;
        for(UUID u:i.userIds())if(!assignments.existsByUserIdAndProfileId(u,p.getId())){
            assignments.save(new UserCompetencyProfileEntity(u,p,CurrentUser.id()));
            n++;
        }
        return n;
    }
    @Transactional
    public long unassignProfile(UUID u,UUID p){
        return assignments.deleteByUserIdAndProfileId(u,p);
    }
    @Transactional
    public AssessmentView assess(AssessmentRequest i){
        UUID actor=CurrentUser.id(),target=i.userId()==null?actor:i.userId();
        if(!target.equals(actor)&&!CurrentUser.authorities().contains(Permissions.COMPETENCIES_ASSESS))throw new ApiException(HttpStatus.FORBIDDEN,"COMPETENCY_SCOPE_DENIED","Không có quyền đánh giá người dùng khác");
        if(i.source()==AssessmentSource.SELF&&!target.equals(actor))throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_ASSESSMENT_SOURCE","Đánh giá SELF chỉ áp dụng cho chính người đang đăng nhập");
        var c=competencies.findById(i.competencyId()).orElseThrow(()->notFound("COMPETENCY_NOT_FOUND","Không tìm thấy năng lực"));
        if(i.level()>c.getMaxLevel())throw new ApiException(HttpStatus.BAD_REQUEST,"LEVEL_EXCEEDS_MAX","Mức năng lực vượt quá giới hạn "+c.getMaxLevel());
        var e=assessments.save(new UserCompetencyAssessmentEntity(target,c,i.level(),i.source(),actor,i.evidenceJson(),i.validUntil()));
        events.publish(EventTypes.COMPETENCY_ASSESSED,"competency-service",e.getId().toString(),Map.of("assessmentId",e.getId(),"userId",target,"competencyId",c.getId(),"level",i.level(),"source",i.source().name()));
        return view(e);
    }
    @Transactional
    public void mapCourse(CourseMapRequest i){
        var c=competencies.findById(i.competencyId()).orElseThrow(()->notFound("COMPETENCY_NOT_FOUND","Không tìm thấy năng lực"));
        if(i.targetLevel()>c.getMaxLevel())throw new ApiException(HttpStatus.BAD_REQUEST,"LEVEL_EXCEEDS_MAX","Mức mục tiêu vượt giới hạn năng lực");
        var e=courseMaps.findByCourseIdAndCompetencyId(i.courseId(),c.getId());
        if(e==null)e=new CourseCompetencyMapEntity(i.courseId(),c);
        e.setTargetLevel(i.targetLevel());
        courseMaps.save(e);
    }
    @Transactional(readOnly=true)
    public CompetencyGapResponse gap(UUID userId){
        var pas=assignments.findAllByUserId(userId);
        var pids=pas.stream().map(x->x.getProfile().getId()).toList();
        Map<UUID,CompetencyProfileRequirementEntity> req=new LinkedHashMap<>();
        for(UUID pid:pids)for(var r:requirements.findAllByProfileId(pid))req.merge(r.getCompetency().getId(),r,(x,y)->x.getRequiredLevel()>=y.getRequiredLevel()?x:y);
        Instant now=Instant.now();
        Map<UUID,UserCompetencyAssessmentEntity> latest=new LinkedHashMap<>();
        for(var a:assessments.findAllByUserIdOrderByAssessedAtDesc(userId))if((a.getValidUntil()==null||a.getValidUntil().isAfter(now))&&!latest.containsKey(a.getCompetency().getId()))latest.put(a.getCompetency().getId(),a);
        List<GapRow> rows=new ArrayList<>();
        for(var r:req.values()){
            int cur=latest.containsKey(r.getCompetency().getId())?latest.get(r.getCompetency().getId()).getLevel():0;
            int gap=Math.max(r.getRequiredLevel()-cur,0);
            var rec=courseMaps.findAllByCompetencyId(r.getCompetency().getId()).stream().filter(x->x.getTargetLevel()>=r.getRequiredLevel()).map(CourseCompetencyMapEntity::getCourseId).distinct().toList();
            rows.add(new GapRow(r.getCompetency().getId(),r.getCompetency().getCode(),r.getCompetency().getName(),r.getCompetency().getCategory(),cur,r.getRequiredLevel(),gap,r.getWeight(),rec));
        }
        rows.sort(Comparator.<GapRow>comparingDouble(x->-(x.gap()*x.weight())).thenComparing(GapRow::name));
        double total=rows.stream().mapToDouble(x->x.requiredLevel()*x.weight()).sum(),ach=rows.stream().mapToDouble(x->Math.min(x.currentLevel(),x.requiredLevel())*x.weight()).sum();
        double ready=total<=0?100:Math.max(0,Math.min(100,ach*100/total));
        return new CompetencyGapResponse(userId,pids,ready,rows,now);
    }
    @Transactional(readOnly=true)
    public List<AssessmentView> assessments(UUID u){
        return assessments.findAllByUserIdOrderByAssessedAtDesc(u).stream().map(this::view).toList();
    }
    private void replaceRequirements(CompetencyProfileEntity p,List<RequirementRequest> rows){
        Set<UUID> seen=new HashSet<>();
        for(var r:rows)if(!seen.add(r.competencyId()))throw new ApiException(HttpStatus.BAD_REQUEST,"DUPLICATE_REQUIREMENT","Một năng lực chỉ được xuất hiện một lần trong khung");
        requirements.deleteAllByProfileId(p.getId());
        for(var r:rows){
            var c=competencies.findById(r.competencyId()).orElseThrow(()->notFound("COMPETENCY_NOT_FOUND","Không tìm thấy năng lực "+r.competencyId()));
            if(r.requiredLevel()>c.getMaxLevel())throw new ApiException(HttpStatus.BAD_REQUEST,"LEVEL_EXCEEDS_MAX","Mức yêu cầu vượt giới hạn của "+c.getCode());
            requirements.save(new CompetencyProfileRequirementEntity(p,c,r.requiredLevel(),r.weight()));
        }

    }
    private ProfileView profileView(CompetencyProfileEntity p){
        return new ProfileView(p.getId(),p.getCode(),p.getName(),p.getDescription(),p.getOrganizationUnitId(),p.getRoleCode(),p.isActive(),requirements.findAllByProfileId(p.getId()).stream().map(r->new RequirementView(r.getCompetency().getId(),r.getCompetency().getCode(),r.getCompetency().getName(),r.getRequiredLevel(),r.getWeight())).toList());
    }
    private CompetencyView view(CompetencyEntity e){
        return new CompetencyView(e.getId(),e.getCode(),e.getName(),e.getDescription(),e.getCategory(),e.getMaxLevel(),e.getStatus());
    }
    private AssessmentView view(UserCompetencyAssessmentEntity e){
        return new AssessmentView(e.getId(),e.getUserId(),e.getCompetency().getId(),e.getCompetency().getCode(),e.getCompetency().getName(),e.getLevel(),e.getSource(),e.getAssessedBy(),e.getAssessedAt(),e.getValidUntil());
    }
    private ApiException conflict(String m){
        return new ApiException(HttpStatus.CONFLICT,"COMPETENCY_CONFLICT",m);
    }
    private ApiException notFound(String c,String m){
        return new ApiException(HttpStatus.NOT_FOUND,c,m);
    }
    private String trim(String s){
        return s==null?null:s.trim();
    }
    private String upper(String s){
        return s==null?null:s.trim().toUpperCase();
    }

}
