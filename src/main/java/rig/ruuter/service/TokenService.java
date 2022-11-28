package rig.ruuter.service;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import rig.commons.aop.Timed;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@Timed
public class TokenService {

    private final static Random random = new SecureRandom();
    private final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

    public TokenService() throws NoSuchAlgorithmException {
    }

    public String generateToken(Integer length) {
        return RandomStringUtils.random(length, 0, 0, true, true, null, random);
    }

    public String generateHash(String input) {
        messageDigest.update(input.getBytes());
        return Base64.getEncoder().encodeToString(messageDigest.digest());
    }

    public String generateUuid() {
        return UUID.randomUUID().toString();
    }
}
