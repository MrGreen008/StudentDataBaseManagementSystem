package edu.vishvaya.portal.service;

import edu.vishvaya.portal.api.RegistrationRequest;

public interface RegistrationService {
    void register(RegistrationRequest request);
    void recover(String identifier,String role);
    boolean reset(String identifier,String role,String code,String password);
    void contact(java.util.UUID account,String email,String password);
    java.util.Map<String,Object> profile(java.util.UUID account);
}
