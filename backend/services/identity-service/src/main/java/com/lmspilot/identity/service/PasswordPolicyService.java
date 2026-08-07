package com.lmspilot.identity.service;

import com.lmspilot.identity.domain.*;

import com.lmspilot.support.api.ApiException;

import java.time.Instant;

import org.springframework.http.HttpStatus;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
@Service
public class PasswordPolicyService {
    private final PasswordEncoder encoder;
    private final PasswordHistoryRepository history;
    public PasswordPolicyService(PasswordEncoder encoder,PasswordHistoryRepository history){
        this.encoder=encoder;
        this.history=history;
    }
    public void validate(String password){
        if(password==null||password.length()<12||password.length()>128||!password.matches(".*[A-Z].*")||!password.matches(".*[a-z].*")||!password.matches(".*\\d.*")||!password.matches(".*[^A-Za-z0-9].*"))throw new ApiException(HttpStatus.BAD_REQUEST,"WEAK_PASSWORD","Mật khẩu phải dài 12–128 ký tự và có chữ hoa, chữ thường, số, ký tự đặc biệt");
    }
    @Transactional
    public void change(UserAccountEntity user,String raw,boolean forceChange,String reason){
        validate(raw);
        if(encoder.matches(raw,user.passwordHash)||history.findTop10ByUserIdOrderByCreatedAtDesc(user.id).stream().anyMatch(h->encoder.matches(raw,h.passwordHash)))throw new ApiException(HttpStatus.CONFLICT,"PASSWORD_REUSED","Không được sử dụng lại mật khẩu gần đây");
        if(user.passwordHash!=null&&!user.passwordHash.isBlank()){
            PasswordHistoryEntity h=new PasswordHistoryEntity();
            h.userId=user.id;
            h.passwordHash=user.passwordHash;
            history.save(h);
        }
        user.passwordHash=encoder.encode(raw);
        user.passwordChangedAt=Instant.now();
        user.mustChangePassword=forceChange;
        user.failedLoginCount=0;
        user.lockedUntil=null;
        user.updatedAt=Instant.now();
    }

}
