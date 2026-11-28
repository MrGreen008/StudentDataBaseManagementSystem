package edu.vishvaya.portal.service;

import edu.vishvaya.portal.api.PortalNoticeRequest;
import java.util.*;

public interface PortalNoticeService {
    List<Map<String,Object>> inbox(UUID account);
    List<Map<String,Object>> managed(UUID actor);
    void save(UUID id, PortalNoticeRequest notice, UUID actor);
}
