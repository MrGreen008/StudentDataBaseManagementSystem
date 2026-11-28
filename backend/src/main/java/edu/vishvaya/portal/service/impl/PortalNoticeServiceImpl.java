package edu.vishvaya.portal.service.impl;

import edu.vishvaya.portal.api.PortalNoticeRequest;
import edu.vishvaya.portal.persistence.*;
import edu.vishvaya.portal.service.PortalNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
public class PortalNoticeServiceImpl implements PortalNoticeService {
    private final PortalNoticeRepository notices;
    private final AccountRepository accounts;
    public PortalNoticeServiceImpl(PortalNoticeRepository notices, AccountRepository accounts) {
        this.notices=notices;
        this.accounts=accounts;
    }
    @Override public List<Map<String,Object>> inbox(UUID account) { return notices.inbox(account); }

    @Override public List<Map<String,Object>> managed(UUID actor) {
        var account = accounts.byId(actor).orElseThrow();
        return notices.managed(actor, account.role().equals("ADMIN"));
    }

    @Override @Transactional
    public void save(UUID id, PortalNoticeRequest notice, UUID actor) {
        notices.lockIdentity();
        var account = accounts.byId(actor).orElseThrow();
        boolean admin = account.role().equals("ADMIN");
        if (!account.enabled() || (!admin && !account.role().equals("FACULTY"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!admin && (!notices.owns(id, actor) || !notice.audience().equals("STUDENT")
                || notice.cohortCode() == null || !notices.teaches(actor, notice.cohortCode())
                || notice.accountId() != null)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Faculty publishers can manage their own notices for assigned cohorts.");
        }
        if (notice.accountId() != null && !accounts.byId(notice.accountId())
                .map(target -> target.role().equals(notice.audience())).orElse(false)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an account with the target role.");
        }
        notices.save(id, notice, actor);
        notices.reviewState(id, admin?"APPROVED":notice.published()?"PENDING":"DRAFT",
                admin && notice.published(),actor);
    }
}
