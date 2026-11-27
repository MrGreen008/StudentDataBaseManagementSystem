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
      throw new Error('Sign-in failed or your session expired. Sign in again.');
    }
    if (response.status === 429) throw new Error('Too many sign-in attempts. Try again in one minute.');
    throw new Error(body.detail || 'The request could not be completed. Reload and try again.');
  }
  return response.status === 204 ? null : response.json();
}
async function loadAccount() {
  const response = await fetch('/api/auth/me', {cache: 'no-store'});
  const signedIn = response.ok;
  if (!signedIn) showSignIn();
  document.querySelector('#login').hidden = signedIn;
  document.querySelector('#account').hidden = !signedIn;
  if (signedIn) {
    const account = await response.json();
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
  const button = form.querySelector('button');
  button.disabled = true;
  try { await action(); } catch (error) { status.textContent = error.message; }
  finally { button.disabled = false; }
}
document.querySelector('#login').addEventListener('submit', event => {
  event.preventDefault();
  const form = event.currentTarget;
  submit(form, async () => {
    await csrf();
    await request('/api/auth/login', {method: 'POST',
      headers: {[token.headerName]: token.token}, body: new URLSearchParams(new FormData(form))});
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
  for (const field of fields) {
    const label = element('label', field.replaceAll('_', ' '));
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
      : field === 'temporaryPassword' ? 'password' : 'text';
    input.required = input.type !== 'checkbox' && field !== 'expires_at';
    if (input.type === 'checkbox') input.checked = values[field] ?? ['active', 'visible', 'enabled'].includes(field);
    else input.value = values[field] ?? '';
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
function records(container, rows, choose) {
  container.replaceChildren();
  if (!rows.length) container.append(element('p', 'No records available.'));
  for (const row of rows) {
    const article = element('article');
    article.append(element('h4', row.displayName || row.name || row.title || row.login || row.code));
    const details = element('dl');
    for (const [key, value] of Object.entries(row)
      .filter(([key]) => !['created_at', 'updated_at', 'created_by', 'updated_by'].includes(key))
    ) {
      details.append(element('dt', key.replaceAll('_', ' ')),
        element('dd', value === null ? '—' : typeof value === 'boolean' ? (value ? 'Yes' : 'No') : String(value)));
    }
    article.append(details);
    if (choose) {
      const button = element('button', 'Edit');
      button.type = 'button';
      button.addEventListener('click', () => choose(row));
      article.append(button);
    }
    container.append(article);
  }
}
async function loadDashboard(account) {
  const dashboard = document.querySelector('#dashboard');
  dashboard.replaceChildren(element('h2', account.role + ' dashboard'));
  if (account.role === 'ADMIN') {
    await collegeTools(dashboard);
    await accountTools(dashboard);
  } else {
    const rows = await request('/api/' + account.role.toLowerCase() + '/dashboard');
    records(dashboard.appendChild(element('section')), rows);
  }
  if (account.role === 'ADMIN' || account.canPublish) await noticeTools(dashboard);
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
        panels.forEach(item => { item.hidden = item !== panel; });
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
    function edit(row = {}) {
      formArea.hidden = false;
      const heading = element('h4', row.code ? 'Edit ' + row.name : 'Add ' + type);
      formArea.replaceChildren(editor(schema[type], row, async body => {
        await mutate('/api/admin/setup/' + type, body);
        await load();
      }));
      formArea.prepend(heading);
      if (row.code) formArea.querySelector('[name=code]').readOnly = true;
      revealEditor(formArea);
    }
    editRecord = edit;
    formArea.hidden = true;
    list.replaceChildren();
    const rows = await request('/api/admin/setup/' + type);
    if (select.value === type) records(list, rows, edit);
  }
  select.addEventListener('change', () => load().catch(error => { status.textContent = error.message; }));
  newRecord.addEventListener('click', () => editRecord());
  section.append(element('p', 'Choose a record type to browse existing records or add a new one.'),
    select, newRecord, formArea, list);
  container.append(section);
  await load();
}
async function accountTools(container) {
  const section = element('section');
  section.append(element('h3', 'Accounts'));
  const list = element('div');
  const editArea = element('div');
  async function load() {
    records(list, await request('/api/admin/accounts'), account => {
      const permissions = editor(['role', 'enabled', 'canPublish'], account, async body => {
        await mutate('/api/admin/accounts/' + account.id + '/permissions', body);
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
          published: body.published, publishedAt: body.published_at, expiresAt: body.expires_at || null
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
  section.append(add, element('p', 'Publication and expiry times include a timezone. Example format: 2026-11-27T10:00:00+05:30.'),
    formArea, list);
  container.append(section);
  await load();
}

