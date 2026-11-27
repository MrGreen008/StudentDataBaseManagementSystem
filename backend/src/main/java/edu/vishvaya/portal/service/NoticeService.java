package edu.vishvaya.portal.service;

import edu.vishvaya.portal.api.NoticeRequest;
import java.util.*;

public interface NoticeService {
    List<Map<String, Object>> list();
    void save(UUID id, NoticeRequest request, UUID actor);
}
