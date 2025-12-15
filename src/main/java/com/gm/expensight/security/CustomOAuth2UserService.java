package com.gm.expensight.security;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        String oauthUserId = extractUserId(attributes, registrationId);
        String email = (String) attributes.get("email");

        if (oauthUserId == null || email == null) {
            log.error("Missing required OAuth2 attributes - userId: {}, email: {}", oauthUserId, email);
            throw new OAuth2AuthenticationException("Missing required OAuth2 attributes: userId or email");
        }

        String nameAttributeKey = getNameAttributeKey(registrationId);
        return new DefaultOAuth2User(
                oauth2User.getAuthorities().stream().map(a -> new SimpleGrantedAuthority("ROLE_USER")).toList(),
                attributes,
                nameAttributeKey
        );
    }

    private String extractUserId(Map<String, Object> attributes, String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> (String) attributes.get("sub");
            case "github" -> String.valueOf(attributes.get("id"));
            case "microsoft" -> (String) attributes.get("sub");
            default -> (String) attributes.getOrDefault("sub", String.valueOf(attributes.get("id")));
        };
    }

    private String getNameAttributeKey(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> "sub";
            case "github" -> "id";
            case "microsoft" -> "sub";
            default -> "sub";
        };
    }
}


