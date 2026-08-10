package com.aiecomm.camp.security;

import com.aiecomm.camp.entity.AuthProvider;
import com.aiecomm.camp.entity.User;
import com.aiecomm.camp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CustomOAuth2 extends DefaultOAuth2UserService {


    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User googleUser = super.loadUser(userRequest);

        String userEmail = googleUser.getAttribute("email");
        if (userEmail != null && userRepository != null) {
            Optional<User> user = userRepository.findByEmail(userEmail.trim().toLowerCase());
            if (user.isPresent() && user.get().getProvider() == AuthProvider.LOCAL) {
                throw new OAuth2AuthenticationException("An account already exists with email " + userEmail + " using Email/Password. Please log in using your email and password.");
            }
        }

        return googleUser;
    }
}


