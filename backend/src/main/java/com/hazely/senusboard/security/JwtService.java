package com.hazely.senusboard.security;

import com.hazely.senusboard.entities.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
@AllArgsConstructor
public class JwtService {
    private final JwtConfig jwtConfig;

    public Jwt generateAccessToken(UserEntity user){

        return generateToken(user, jwtConfig.getAccessTokenExpiration(), "ACCESS");
    }

    public Jwt generateRefreshToken(UserEntity user){

        return generateToken(user, jwtConfig.getRefreshTokenExpiration(), "REFRESH");
    }

    private Jwt generateToken(UserEntity user, int tokenExpiration, String type) {
        var claims = Jwts.claims()
                .subject(user.getId().toString())
                .add("id", user.getId())
                .add("email",user.getEmail())
                .add("name",user.getName())
                .add("role", user.getRole().name())
                .add("status", user.getStatus().name())
                .add("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * tokenExpiration))
                .build();


        return new Jwt(claims, jwtConfig.getSecretKey());
    }

    public Jwt parseToken(String token){
        try {
            var claims=getClaims(token);
            return new Jwt(claims, jwtConfig.getSecretKey());
        } catch (Exception e) {
            return null;
        }
    }


    private Claims getClaims(String token) {
        var claims = Jwts.parser()
                .verifyWith(jwtConfig.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims;
    }

}
