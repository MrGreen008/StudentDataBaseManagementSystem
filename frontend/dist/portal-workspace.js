const rolePresentation = {
  STUDENT: {image:'gathering-hall.jpg',alt:'Generated college assembly hall filled with students',caption:'A place to learn. A community to belong to.'},
  FACULTY: {image:'student-project-lab.jpg',alt:'Generated teaching and research laboratory',caption:'Guide ideas. Inspire the next generation.'},
  ADMIN: {image:'administration.jpg',alt:'Generated college administration building',caption:'Bring your campus together.'}
};
const noticeTopics = {GENERAL:'General notice',TIMETABLE:'Timetable',EXAM_DATES:'Exam dates',HOLIDAY:'Holiday'};
function setPortalRole(role) {
  const selected = Object.hasOwn(rolePresentation, role) ? role : 'STUDENT';
  const presentation = rolePresentation[selected];
  document.body.dataset.portalRole = selected;
  const address=new URL(location.href);
  address.searchParams.set('role',selected);
  history.replaceState(null,'',address);
  const image = document.querySelector('#portal-photo');
  image.src = '/images/' + presentation.image;
  image.alt = presentation.alt;
  document.querySelector('#portal-caption').textContent = presentation.caption;
  document.querySelector('#login h2').textContent =
    ({STUDENT:'Student sign in',FACULTY:'Faculty sign in',ADMIN:'Administration sign in'})[selected];
  document.querySelectorAll('[data-role]').forEach(button =>
    button.setAttribute('aria-pressed', String(button.dataset.role === selected)));
  const register=document.querySelector('#open-register');
  if(register) register.hidden=selected==='ADMIN';
  const note=document.querySelector('#demo-registration-note');
  if(note) note.hidden=selected==='ADMIN';
  const service=document.querySelector('#self-service');
  if(service) {service.hidden=true;service.replaceChildren();}
}
document.querySelectorAll('[data-role]').forEach(button =>
  button.addEventListener('click', () => setPortalRole(button.dataset.role)));
setPortalRole(new URLSearchParams(location.search).get('role'));

function fieldLabel(value) {
  const labels = {displayName:'Display name',passwordChangeRequired:'Password change required',
    canPublish:'Publishing access',account_id:'Account',cohort_code:'Cohort',
    published_at:'Published',expires_at:'Expires'};
  return labels[value] || value.replaceAll('_',' ').replace(/([a-z])([A-Z])/g,'$1 $2');
}
function displayValue(key, value) {
  if (value === null || value === undefined) return '—';
  if (typeof value === 'boolean') return value ? 'Yes' : 'No';
  if (key.endsWith('_at') && !Number.isNaN(Date.parse(value))) {
    return new Date(value).toLocaleString('en-IN',{dateStyle:'medium',timeStyle:'short'});
  }
  return String(value);
}
function renderTable(container, rows, choose, {fields, actionLabel = 'Edit', emptyMessage = 'No records yet.'} = {}) {
  container.replaceChildren();
  const search = element('input');
  search.type = 'search'; search.placeholder = 'Search records…';
  search.setAttribute('aria-label','Search records');
  const count = element('p'); count.className = 'record-count';
  const wrapper = element('div'); wrapper.className = 'table-scroll';
  wrapper.tabIndex = 0; wrapper.setAttribute('aria-label','Records');
  const table = element('table'), caption = element('caption','Records');
  const columns = fields || [...new Set(rows.flatMap(row => Object.keys(row)))]
    .filter(key => !['created_by','updated_by','created_at','updated_at','id','passwordChangeRequired','canPublish','action_url'].includes(key));
  const head = element('thead'), tr = element('tr');
  for (const key of columns) { const th = element('th',fieldLabel(key)); th.scope='col'; tr.append(th); }
  if (choose) tr.append(element('th','Actions'));
  head.append(tr); table.append(caption, head);
  const body = element('tbody'); table.append(body); wrapper.append(table);
  const empty = element('p'); empty.className='empty-state';
  function draw() {
    const query = search.value.trim().toLowerCase();
    const filtered = rows.filter(row => Object.values(row).some(value => String(value ?? '').toLowerCase().includes(query)));
    count.textContent = filtered.length + ' of ' + rows.length + ' records';
    body.replaceChildren();
    for (const row of filtered) {
      const line=element('tr');
      for (const key of columns) line.append(element('td',displayValue(key,row[key])));
      if (choose) {
        const action=element('td');
        const label=typeof actionLabel === 'function' ? actionLabel(row) : actionLabel;
        if (label) {
          const edit=element('button',label);
          edit.type='button';
          const description=row.name || row.title || row.login || row.code || row.course || row.student || row.instructor;
          if (description) edit.setAttribute('aria-label',label + ' ' + description);
          edit.addEventListener('click',()=>choose(row));
          action.append(edit);
        }
        line.append(action);
      }
      body.append(line);
    }
    empty.textContent = rows.length ? 'No records match your search.' : emptyMessage;
    empty.hidden = filtered.length > 0; wrapper.hidden = !filtered.length;
  }
  search.addEventListener('input',draw);
  container.append(search,count,wrapper,empty); draw();
}
function replaceWithSelect(form, field, choices, selected, optional = false) {
  const original=form.elements.namedItem(field);
  if (!original) return;
  const select=element('select'); select.name=field; select.required=!optional;
  const blank=element('option',optional ? 'All / no specific target' : 'Select an option');
  blank.value=''; select.append(blank);
  for(const choice of choices) {
    const option=element('option',choice.label); option.value=choice.value; select.append(option);
  }
  // Keep retired references available while editing historical records.
  if (selected && !choices.some(choice => choice.value === selected)) {
    const previous=element('option',selected + ' (existing reference)');
    previous.value=selected; select.append(previous);
  }
  select.value=selected || '';
  original.replaceWith(select);
}
async function populateReferences(form, row, type) {
  const references={department_code:'departments',program_code:'programs',cohort_code:'cohorts',
    course_code:'courses',term_code:'terms',faculty_code:'faculty',offering_code:'offerings',account_id:'accounts'};
  for(const [field,resource] of Object.entries(references)) {
    if(!form.elements.namedItem(field)) continue;
    const url=resource==='accounts'?'/api/admin/accounts':'/api/admin/setup/'+resource;
    let rows=await request(url);
    if(resource==='accounts') rows=rows.filter(account=>account.role===(type==='students'?'STUDENT':'FACULTY'));
    replaceWithSelect(form,field,rows.map(item=>({value:resource==='accounts'?item.id:item.code,
      label:(item.displayName || item.name) + ' · ' + (item.login || item.code)})),row[field]);
  }
}
async function overviewPanel(container, account, existing) {
  const section=existing || element('section'); section.className='overview-panel';
  section.replaceChildren();
  section.refresh=()=>overviewPanel(container,account,section);
  section.append(element('h3','Overview'));
  const banner=element('div'); banner.className='dashboard-banner';
  const image=element('img'); image.src='/images/'+rolePresentation[account.role].image; image.alt=rolePresentation[account.role].alt;
  const text=element('div');
  text.append(element('p',new Date().toLocaleDateString('en-IN',{weekday:'long',day:'numeric',month:'long'})),
    element('h4','Welcome, '+account.displayName),element('p',account.role==='STUDENT'?'Your academic profile and campus updates, in one place.':
      account.role==='FACULTY'?'Your teaching assignments and updates from campus.':'Manage your college community and keep everyone informed.'));
  banner.append(image,text); section.append(banner);
  const inbox=await request('/api/portal/notices');
  section.append(element('h4','Latest notices'));
  if(!inbox.length) section.append(element('p','No new notices for your account.'));
  for(const notice of inbox) section.append(noticeCard(notice));
  if(!existing) container.append(section);
}
function noticeCard(notice) {
  const article=element('article'); article.className='inbox-notice';
  const topic=noticeTopics[notice.topic] || noticeTopics.GENERAL;
  article.append(element('small',topic+' · '+displayValue('published_at',notice.published_at)),
    element('h4',notice.title),element('p',notice.summary));
  article.append(element('small',notice.approval_status+(notice.published?' · Published':' · Unpublished')));
  if(notice.attachment_name) {
    const download=element('a','Download '+notice.attachment_name);
    download.href='/api/portal/notices/'+notice.id+'/attachment';
    download.className='document-link';
    article.append(download);
  }
  return article;
}
async function portalNoticeTools(container, account) {
  const section=element('section'); section.append(element('h3','Portal notices'));
  const add=element('button','Create portal notice'); add.type='button';
  const area=element('div'), list=element('div');
  const admin=account.role==='ADMIN';
  async function edit(row={}) {
    const cohorts=await request(admin?'/api/admin/setup/cohorts':'/api/faculty/dashboard');
    const targets=admin?await request('/api/admin/accounts'):[];
    const fields=['title','summary','topic','audience','cohort_code','account_id','published','published_at','expires_at'];
    const id=row.id || crypto.randomUUID();
    const form=editor(fields,{audience:'STUDENT',published_at:new Date().toISOString(),...row},async body=>{
      await mutate('/api/staff/portal-notices/'+id,{title:body.title,summary:body.summary,topic:body.topic,audience:body.audience,
        cohortCode:body.cohort_code || null,accountId:body.account_id || null,published:body.published,
        publishedAt:new Date(body.published_at).toISOString(),expiresAt:body.expires_at?new Date(body.expires_at).toISOString():null});
      if(attachment.files.length) {
        await csrf();
        const data=new FormData();data.append('file',attachment.files[0]);
        const response=await fetch('/api/staff/portal-notices/'+id+'/attachment',
          {method:'POST',headers:{[token.headerName]:token.token},body:data});
        if(!response.ok) {
          const problem=await response.json().catch(()=>({}));
          throw new Error(problem.detail || 'Notice saved, but attachment upload failed.');
        }
        if(!admin && body.published) {
          await mutate('/api/staff/portal-notices/'+id,{title:body.title,summary:body.summary,topic:body.topic,audience:body.audience,
            cohortCode:body.cohort_code || null,accountId:null,published:true,
            publishedAt:new Date(body.published_at).toISOString(),expiresAt:body.expires_at?new Date(body.expires_at).toISOString():null});
        }
      }
      await load();
    });
    replaceWithSelect(form,'topic',Object.entries(noticeTopics).map(([value,label])=>({value,label})),row.topic || 'GENERAL');
    const attachment=element('input');attachment.type='file';attachment.accept='.pdf,application/pdf';
    const attachmentLabel=element('label','Optional official document · PDF, up to 5 MB');
    attachmentLabel.append(attachment);form.insertBefore(attachmentLabel,form.querySelector('button'));
    if(row.attachment_name) form.insertBefore(element('p','Attached: '+row.attachment_name),attachmentLabel);
    if(!admin) form.elements.published.closest('label').firstChild.textContent='Submit for administrator approval';
    replaceWithSelect(form,'audience',(admin?['STUDENT','FACULTY','ADMIN']:['STUDENT']).map(value=>({value,label:value})),row.audience || 'STUDENT');
    replaceWithSelect(form,'cohort_code',[...new Map(cohorts.map(item=>[admin?item.code:item.cohort_code,
      {value:admin?item.code:item.cohort_code,label:item.name || item.cohort_code}])).values()],row.cohort_code,admin);
    replaceWithSelect(form,'account_id',targets.map(item=>({value:item.id,label:item.displayName+' · '+item.login})),row.account_id,true);
    if(!admin) form.elements.account_id.closest('label').hidden=true;
    function audienceChanged() {
      const student=form.elements.audience.value==='STUDENT';
      form.elements.cohort_code.closest('label').hidden=!student;
      if(!student) form.elements.cohort_code.value='';
      replaceWithSelect(form,'account_id',targets.filter(item=>item.role===form.elements.audience.value)
        .map(item=>({value:item.id,label:item.displayName+' · '+item.login})),form.elements.account_id.value,true);
      if(!admin) form.elements.account_id.closest('label').hidden=true;
    }
    form.elements.audience.addEventListener('change',audienceChanged);audienceChanged();
    const templates=element('select');
    templates.setAttribute('aria-label','Notice template');
    for(const title of ['Write a notice','Exam forms open – Semester 2','Final-year graduation seminar']) {
      const option=element('option',title);option.value=title;templates.append(option);
    }
    templates.addEventListener('change',()=>{
      if(templates.selectedIndex === 0) return;
      form.elements.title.value=templates.value;
      form.elements.summary.value=templates.selectedIndex===1
        ? 'Draft example. Add the verified application dates, eligibility and submission instructions before publishing.'
        : 'Draft example. Add the confirmed seminar date, venue and graduating cohort before publishing.';
      form.elements.published.checked=false;
    });
    area.hidden=false; area.replaceChildren(element('h4',row.id?'Edit portal notice':'New portal notice'),
      element('p','Choose a template to start a draft, then verify the details and select recipients.'),templates,form);revealEditor(area);
  }
  async function load() {
    area.hidden=true;
    const rows=await request('/api/staff/portal-notices');
    records(list,rows,row=>edit(row).catch(error=>{status.textContent=error.message;}));
    if(admin) {
      for(const row of rows.filter(item=>item.approval_status==='PENDING')) {
        const review=element('article');review.append(element('h4','Review: '+row.title),element('p',row.summary));
        if(row.attachment_name) {
          const link=element('a','Download attachment');link.href='/api/portal/notices/'+row.id+'/attachment';review.append(link);
        }
        for(const approved of [true,false]) {
          const button=element('button',approved?'Approve publication':'Reject submission');button.type='button';
          button.addEventListener('click',async()=>{
            button.disabled=true;
            try {await mutate('/api/admin/notices/'+row.id+'/review',{revision:row.revision,approved},'POST');await load();}
            catch(error){status.textContent=error.message;button.disabled=false;}
          });
          review.append(button);
        }
        list.append(review);
      }
    }
  }
  add.addEventListener('click',()=>edit().catch(error=>{status.textContent=error.message;}));
  section.append(element('p',admin?'Choose a role, cohort or individual recipient. These notices stay inside the portals.':
    'Submit notices for students in your assigned cohorts. An administrator reviews each publication.'),add,area,list);
  section.refresh=load;
  container.append(section);await load();
}
