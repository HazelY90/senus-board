package com.hazely.senusboard.security;


import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.Getter;

import javax.crypto.SecretKey;
import java.util.Date;

@Getter
public class Jwt {

    private final Claims claims;
    private final SecretKey key;

    public Jwt(Claims claims, SecretKey key) {
        this.claims = claims;
        this.key = key;
    }

    @Override
    public String toString() {
        return Jwts.builder()
                .claims(claims)
                .signWith(key)
                .compact();
    }

    public boolean isExpired() {
        try {
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return false;
        }
    }

    public Long getId() {
        return Long.valueOf(claims.getSubject());
    }

    public Role getRole(){
        return Role.valueOf(claims.get("role",String.class));
    }

    public Status getStatus() {
        return Status.valueOf(claims.get("status", String.class));
    }

    public boolean isAccess() {
        return "ACCESS".equals(claims.get("type", String.class));
    }

    public boolean isRefresh() {
        return "REFRESH".equals(claims.get("type", String.class));
    }
}
