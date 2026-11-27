package edu.vishvaya.portal.service.impl;

import edu.vishvaya.portal.api.NoticeRequest;
import edu.vishvaya.portal.persistence.NoticeRepository;
import edu.vishvaya.portal.service.NoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class NoticeServiceImpl implements NoticeService {
    private final NoticeRepository notices;
    public NoticeServiceImpl(NoticeRepository notices) { this.notices = notices; }
    @Override public List<Map<String, Object>> list() { return notices.list(); }
    @Override @Transactional
    public void save(UUID id, NoticeRequest request, UUID actor) { notices.save(id, request, actor); }
}
