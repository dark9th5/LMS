package com.lmspilot.certificate.domain;

import jakarta.persistence.*;

import java.time.Instant;

import java.util.UUID;
@Entity
@Table(name="certificate_templates",indexes=@Index(name="idx_certificate_template_course",columnList="course_id,active,updated_at"))public class CertificateTemplateEntity{
    @Id
    private UUID id=UUID.randomUUID();
    @Column(nullable=false,length=180)
    private String name="Mẫu mặc định";
    @Column(name="course_id")
    private UUID courseId;
    @Column(nullable=false,length=240)
    private String title="CHỨNG CHỈ HOÀN THÀNH";
    @Column(name="issuer_name",nullable=false,length=240)
    private String issuerName="LMSPilot";
    @Column(name="body_text",nullable=false,length=1000)
    private String bodyText="Xác nhận người học đã hoàn thành chương trình đào tạo.";
    @Column(name="primary_color",nullable=false,length=20)
    private String primaryColor="#173b65";
    @Column(name="secondary_color",nullable=false,length=20)
    private String secondaryColor="#b99044";
    @Column(name="logo_url",length=500)
    private String logoUrl;
    @Column(name="signature_name",length=240)
    private String signatureName;
    @Column(nullable=false)
    private boolean active=true;
    @Column(name="created_by",nullable=false)
    private UUID createdBy;
    @Column(name="created_at",nullable=false)
    private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false)
    private Instant updatedAt=Instant.now();
    @Version
    private long version;
    protected CertificateTemplateEntity(){
    }
    public CertificateTemplateEntity(String name,UUID courseId,String title,String issuerName,String bodyText,String primaryColor,String secondaryColor,String logoUrl,String signatureName,boolean active,UUID createdBy){
        this.name=name;
        this.courseId=courseId;
        this.title=title;
        this.issuerName=issuerName;
        this.bodyText=bodyText;
        this.primaryColor=primaryColor;
        this.secondaryColor=secondaryColor;
        this.logoUrl=logoUrl;
        this.signatureName=signatureName;
        this.active=active;
        this.createdBy=createdBy;
    }
    public UUID getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public void setName(String v){
        name=v;
    }
    public UUID getCourseId(){
        return courseId;
    }
    public void setCourseId(UUID v){
        courseId=v;
    }
    public String getTitle(){
        return title;
    }
    public void setTitle(String v){
        title=v;
    }
    public String getIssuerName(){
        return issuerName;
    }
    public void setIssuerName(String v){
        issuerName=v;
    }
    public String getBodyText(){
        return bodyText;
    }
    public void setBodyText(String v){
        bodyText=v;
    }
    public String getPrimaryColor(){
        return primaryColor;
    }
    public void setPrimaryColor(String v){
        primaryColor=v;
    }
    public String getSecondaryColor(){
        return secondaryColor;
    }
    public void setSecondaryColor(String v){
        secondaryColor=v;
    }
    public String getLogoUrl(){
        return logoUrl;
    }
    public void setLogoUrl(String v){
        logoUrl=v;
    }
    public String getSignatureName(){
        return signatureName;
    }
    public void setSignatureName(String v){
        signatureName=v;
    }
    public boolean isActive(){
        return active;
    }
    public void setActive(boolean v){
        active=v;
    }
    public Instant getUpdatedAt(){
        return updatedAt;
    }
    public void setUpdatedAt(Instant v){
        updatedAt=v;
    }

}
