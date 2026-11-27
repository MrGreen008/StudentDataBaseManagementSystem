package edu.vishvaya.portal.service;

import edu.vishvaya.portal.api.AccountRequests;
import edu.vishvaya.portal.api.AccountView;
import edu.vishvaya.portal.persistence.PortalAccount;
import java.util.List;
import java.util.UUID;

public interface AccountService {
    void bootstrap(String login, String password);
    List<AccountView> list();
    AccountView create(AccountRequests.Create request, UUID actor);
    void permissions(UUID id, AccountRequests.Permissions request, UUID actor);
    void reset(UUID id, String password, UUID actor);
    void changePassword(UUID id, long version, String currentPassword, String newPassword);
    PortalAccount get(UUID id);
}
