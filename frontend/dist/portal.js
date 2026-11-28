const status = document.querySelector('#status');
let token;
function settings(open) {
  document.querySelector('#settings').hidden = !open;
  document.querySelector('#dashboard').hidden = open;
  document.querySelector('#account-menu').open = false;
}
document.querySelector('#open-settings').addEventListener('click', () => settings(true));
document.querySelector('#close-settings').addEventListener('click', () => settings(false));
function showSignIn() {
  document.body.classList.remove('signed-in');
  document.querySelector('#login-layout').hidden = false;
  document.querySelector('#account-menu').hidden = true;
  document.querySelector('#settings').hidden = true;
  document.querySelector('#dashboard').replaceChildren();
  document.querySelector('#dashboard').hidden = true;
  document.querySelector('#welcome').textContent = '';
  document.querySelector('#role').textContent = '';
  document.querySelector('#password').reset();
  document.querySelector('#account').hidden = true;
  document.querySelector('#login').hidden = false;
  token = undefined;
}
async function csrf() {
  const response = await fetch('/api/auth/csrf', {cache: 'no-store'});
  if (response.status === 401) showSignIn();
  if (!response.ok) throw new Error('Unable to start a secure session. Reload and sign in again.');
  token = await response.json();
}
async function request(url, options = {}) {
  const response = await fetch(url, {cache: 'no-store', ...options});
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    if (response.status === 401) {
      showSignIn();
      throw new Error(url==='/api/auth/login'
        ? 'Check your username or email, password, and selected portal.'
        : 'Your session expired. Sign in again.');
    }
    if (response.status === 429) {
      const seconds=Math.max(1,Number(response.headers.get('Retry-After')) || 60);
      const action=({'/api/auth/login':'sign-in attempts','/api/auth/captcha':'CAPTCHA refreshes',
        '/api/auth/register':'registration attempts','/api/auth/forgot-password':'reset-code requests',
        '/api/auth/reset-password':'reset attempts'})[url] || 'requests';
      const error=new Error('Too many '+action+'. Try again in '+seconds+' seconds.');
      error.retryAfter=seconds;
      throw error;
    }
    throw new Error(body.detail || 'The request could not be completed. Reload and try again.');
  }
  if (response.status === 204) return null;
  const content = await response.text();
  if (!content.trim()) return null;
  try {
    return JSON.parse(content);
  } catch {
    throw new Error('The server returned an unreadable response. Reload the page and try again.');
  }
}
async function loadAccount() {
  const response = await fetch('/api/auth/me', {cache: 'no-store'});
  const signedIn = response.ok;
  if (!signedIn) showSignIn();
  document.querySelector('#login').hidden = signedIn;
  document.querySelector('#account').hidden = !signedIn;
  if (signedIn) {
    const account = await response.json();
    if (document.body.dataset.portalRole !== account.role) {
      showSignIn();
      document.querySelector('#account-menu').hidden=false;
      document.querySelector('#account-label').textContent=account.displayName;
      document.querySelector('#open-settings').hidden=true;
      status.textContent='This account belongs to another portal. Select the correct portal or sign out to switch accounts.';
      return;
    }
    document.body.classList.add('signed-in');
    document.querySelector('#login-layout').hidden = true;
    setPortalRole(account.role);
    document.querySelector('#welcome').textContent = account.displayName;
    document.querySelector('#role').textContent = account.role;
    document.querySelector('#account-label').textContent = account.displayName;
    document.querySelector('#account-menu').hidden = false;
    document.querySelector('#open-settings').hidden = account.passwordChangeRequired;
    document.querySelector('#close-settings').hidden = account.passwordChangeRequired;
    settings(account.passwordChangeRequired);
    status.textContent = account.passwordChangeRequired
      ? 'Change your temporary password to activate portal access.'
      : 'You are signed in.';
    document.querySelector('#dashboard').hidden = account.passwordChangeRequired;
    if (!account.passwordChangeRequired) await loadDashboard(account);
  }
}
async function submit(form, action) {
  const button = form.querySelector('button[type="submit"], button:not([type])');
  button.disabled = true;
  let feedback=form.querySelector('[data-feedback]');
  if(!feedback) {
    feedback=element('p');feedback.dataset.feedback='';feedback.setAttribute('role','status');form.append(feedback);
  }
  feedback.textContent='';status.textContent='';
  try { await action(); } catch (error) {
    feedback.textContent = error.message;status.textContent = error.message;
  }
  finally { button.disabled = false; }
}
document.querySelector('#login').addEventListener('submit', event => {
  event.preventDefault();
  const form = event.currentTarget;
  submit(form, async () => {
    await csrf();
    const body=new URLSearchParams(new FormData(form));
    body.set('portalRole',document.body.dataset.portalRole);
    await request('/api/auth/login', {method: 'POST',
      headers: {[token.headerName]: token.token}, body});
    form.reset();
    await csrf();
    await loadAccount();
  });
});
document.querySelector('#password').addEventListener('submit', event => {
  event.preventDefault();
  const form = event.currentTarget;
  submit(form, async () => {
    await csrf();
    await request('/api/auth/password', {method: 'POST',
      headers: {'Content-Type': 'application/json', [token.headerName]: token.token},
      body: JSON.stringify(Object.fromEntries(new FormData(form)))});
    form.reset();
    await csrf();
    await loadAccount();
    status.textContent = 'Password changed. Sign in with your new password.';
  });
});
document.querySelector('#logout').addEventListener('click', async () => {
  try {
    await csrf();
    await request('/api/auth/logout', {method: 'POST', headers: {[token.headerName]: token.token}});
    await csrf();
    await loadAccount();
    status.textContent = 'You have signed out.';
  } catch (error) { status.textContent = error.message; }
});
csrf().then(loadAccount).catch(error => { status.textContent = error.message; });

function element(tag, text) {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  return node;
}
function revealEditor(container) {
  container.scrollIntoView({block: 'start', behavior: 'smooth'});
  container.querySelector('input, select, textarea')?.focus({preventScroll: true});
}
function editor(fields, values, save) {
  const form = element('form');
  form.autocomplete = 'off';
  form.addEventListener('input',()=>{
    status.textContent='';
    const feedback=form.querySelector('[data-feedback]');if(feedback) feedback.textContent='';
  });
  for (const field of fields) {
    const label = element('label', fieldLabel(field));
    const input = element(field === 'summary' ? 'textarea' : 'input');
    input.name = field;
    if (field === 'role' || field === 'kind') {
      const choices = field === 'role' ? ['STUDENT', 'FACULTY', 'ADMIN'] : ['HONORS', 'MINOR'];
      const select = element('select');
      select.name = field;
      for (const choice of choices) {
        const option = element('option', choice);
        option.value = choice;
        select.append(option);
      }
      select.value = values[field] || choices[0];
      label.append(select);
      form.append(label);
      continue;
    }
    input.type = ['active', 'visible', 'enabled', 'canPublish', 'published'].includes(field) ? 'checkbox'
      : ['display_order', 'priority'].includes(field) ? 'number'
      : ['starts_on', 'ends_on'].includes(field) ? 'date'
      : ['published_at', 'expires_at'].includes(field) ? 'datetime-local'
      : field === 'temporaryPassword' ? 'password' : 'text';
    input.required = input.type !== 'checkbox' && field !== 'expires_at';
    if (input.type === 'checkbox') input.checked = values[field] ?? ['active', 'visible', 'enabled'].includes(field);
    else input.value = values[field] ?? '';
    if (input.type === 'datetime-local' && values[field]) {
      const date = new Date(values[field]);
      if (!Number.isNaN(date.getTime())) {
        input.value = new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0,16);
      }
    }
    label.append(input);
    form.append(label);
  }
  form.append(element('button', 'Save'));
  form.addEventListener('submit', event => {
    event.preventDefault();
    submit(form, async () => {
      const body = {};
      for (const field of fields) {
        const input = form.elements[field];
        body[field] = input.type === 'checkbox' ? input.checked
          : input.type === 'number' ? Number(input.value) : input.value;
      }
      await save(body);
      status.textContent = 'Saved.';
    });
  });
  return form;
}
async function mutate(url, body, method = 'PUT') {
  await csrf();
  return request(url, {method, headers: {'Content-Type': 'application/json', [token.headerName]: token.token},
    body: JSON.stringify(body)});
}
function records(container, rows, choose) { renderTable(container, rows, choose); }
async function loadDashboard(account) {
  const dashboard = document.querySelector('#dashboard');
  dashboard.replaceChildren(element('h2', account.role + ' dashboard'));
  await overviewPanel(dashboard, account);
  if (account.role === 'ADMIN') {
    await collegeTools(dashboard);
    await accountTools(dashboard);
  } else {
    const profile=element('section');
    profile.append(element('h3','My profile'));
    const details=element('div');profile.append(details);
    records(details,[await request('/api/auth/profile')]);
    dashboard.append(profile);
    const rows = await request('/api/' + account.role.toLowerCase() + '/dashboard');
    const section = element('section');
    section.append(element('h3', account.role === 'STUDENT' ? 'My academic profile' : 'My teaching'));
    records(section.appendChild(element('div')), rows);
    dashboard.append(section);
  }
  if (account.role === 'ADMIN') await noticeTools(dashboard);
  if (account.role === 'ADMIN' || account.role === 'FACULTY') await portalNoticeTools(dashboard, account);
  academicPanels(dashboard, account);
  studentServicePanels(dashboard, account);
  const panels = [...dashboard.children].filter(node => node.tagName === 'SECTION');
  if (panels.length > 1) {
    const navigation = element('nav');
    navigation.className = 'portal-navigation';
    navigation.setAttribute('aria-label', 'Portal sections');
    panels.forEach((panel, index) => {
      panel.id = 'workspace-' + index;
      const button = element('button', panel.querySelector('h3')?.textContent || 'My classes');
      button.type = 'button';
      button.setAttribute('aria-controls', panel.id);
      button.addEventListener('click', () => {
        status.textContent='';
        panels.forEach(item => { item.hidden = item !== panel; });
        panel.refresh?.().catch(error=>{status.textContent=error.message;});
        navigation.querySelectorAll('button').forEach(item => item.setAttribute('aria-current', item === button ? 'page' : 'false'));
      });
      panel.hidden = index !== 0;
      button.setAttribute('aria-current', index === 0 ? 'page' : 'false');
      navigation.append(button);
    });
    dashboard.insertBefore(navigation, panels[0]);
  }
}
async function collegeTools(container) {
  const section = element('section');
  section.append(element('h3', 'College setup'));
  const schema = await request('/api/admin/setup');
  const select = element('select');
  select.setAttribute('aria-label', 'College record type');
  Object.keys(schema).forEach(key => {
    const option = element('option', key);
    option.value = key;
    select.append(option);
  });
  const formArea = element('div');
  const list = element('div');
  const newRecord = element('button', 'Add record');
  newRecord.type = 'button';
  let editRecord;
  async function load() {
    const type = select.value;
    async function edit(row = {}) {
      const heading = element('h4', row.code ? 'Edit ' + row.name : 'Add ' + type);
      const form = editor(schema[type], row, async body => {
        await mutate('/api/admin/setup/' + type, body);
        await load();
      });
      await populateReferences(form, row, type);
      if (select.value !== type) return;
      if (row.code) form.querySelector('[name=code]').readOnly = true;
      formArea.replaceChildren(heading, form);
      formArea.hidden = false;
      revealEditor(formArea);
    }
    editRecord = edit;
    formArea.hidden = true;
    list.replaceChildren();
    const rows = await request('/api/admin/setup/' + type);
    if (select.value === type) records(list, rows, row => edit(row).catch(error => { status.textContent = error.message; }));
  }
  select.addEventListener('change', () => load().catch(error => { status.textContent = error.message; }));
  newRecord.addEventListener('click', () => editRecord().catch(error => { status.textContent = error.message; }));
  section.append(element('p', 'Choose a record type to browse existing records or add a new one.'),
    select, newRecord, formArea, list);
  container.append(section);
  section.refresh=load;
  await load();
}
async function accountTools(container) {
  const section = element('section');
  section.append(element('h3', 'Accounts'));
  const list = element('div');
  const editArea = element('div');
  async function load() {
    records(list, await request('/api/admin/accounts'), account => {
      const permissions = editor(['role', 'enabled'], account, async body => {
        await mutate('/api/admin/accounts/' + account.id + '/permissions', {...body,canPublish:false});
        await load();
      });
      const reset = editor(['temporaryPassword'], {}, async body => {
        await mutate('/api/admin/accounts/' + account.id + '/reset-password', body, 'POST');
        reset.reset();
      });
      editArea.replaceChildren(element('h4', account.login), permissions, reset);
      revealEditor(editArea);
    });
  }
  const create = editor(['login', 'displayName', 'role', 'temporaryPassword'], {role: 'STUDENT'}, async body => {
    await mutate('/api/admin/accounts', body, 'POST');
    create.reset();
    create.hidden = true;
    await load();
  });
  const add = element('button', 'Create account');
  add.type = 'button';
  create.hidden = true;
  add.addEventListener('click', () => { create.hidden = !create.hidden; });
  section.append(add, create, list, editArea);
  container.append(section);
  section.refresh=load;
  await load();
}
async function noticeTools(container) {
  const section = element('section');
  section.append(element('h3', 'Public announcements'));
  const formArea = element('div');
  const list = element('div');
  const fields = ['category', 'title', 'summary', 'priority', 'published', 'published_at', 'expires_at'];
  function edit(row = {}) {
    formArea.hidden = false;
    const id = row.id || crypto.randomUUID();
    formArea.replaceChildren(editor(fields, {published_at: new Date().toISOString(), priority: 0, ...row},
      async body => {
        await mutate('/api/staff/announcements/' + id, {
          category: body.category, title: body.title, summary: body.summary, priority: body.priority,
          published: body.published, publishedAt: new Date(body.published_at).toISOString(),
          expiresAt: body.expires_at ? new Date(body.expires_at).toISOString() : null
        });
        await load();
      }));
    formArea.prepend(element('h4', row.id ? 'Edit announcement' : 'New announcement'));
    revealEditor(formArea);
  }
  async function load() {
    formArea.hidden = true;
    records(list, await request('/api/staff/announcements'), edit);
  }
  const add = element('button', 'Create announcement');
  add.type = 'button';
  add.addEventListener('click', () => edit());
  section.append(add, element('p', 'Publication and expiry times use your device’s local timezone.'),
    formArea, list);
  container.append(section);
  section.refresh=load;
  await load();
}

