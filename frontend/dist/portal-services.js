const examStates = {DRAFT: 'Draft', SCHEDULED: 'Scheduled', CANCELLED: 'Cancelled', RELEASED: 'Results published'};
const serviceKinds = {
  BACKLOG: 'Backlog registration', REVALUATION: 'Revaluation', RESULT_CORRECTION: 'Result correction',
  BONA_FIDE: 'Bona fide certificate', SCHOLARSHIP: 'Scholarship guidance', ACCOUNT_SUPPORT: 'Account support'
};
const serviceStates = {
  SUBMITTED: 'Submitted', IN_REVIEW: 'In review', APPROVED: 'Approved', REJECTED: 'Rejected', COMPLETED: 'Completed'
};
const generalServices = ['BONA_FIDE', 'SCHOLARSHIP', 'ACCOUNT_SUPPORT'];
const examFields = ['title', 'course', 'kind_label', 'start_time', 'end_time', 'room', 'exam_status'];

function studentServicePanels(dashboard, account) {
  academicSection(dashboard, 'Exam schedules', content => examsPanel(content, account.role));
  if (account.role === 'ADMIN') {
    academicSection(dashboard, 'Student requests', content => serviceRequestsPanel(content, true));
    academicSection(dashboard, 'Office guidance', content => serviceOfficesPanel(content, true));
  } else if (account.role === 'STUDENT') {
    academicSection(dashboard, 'Results', resultsPanel);
    academicSection(dashboard, 'Student requests', content => serviceRequestsPanel(content, false));
    academicSection(dashboard, 'Help & offices', content => serviceOfficesPanel(content, false));
  }
}

function examsPanel(content, role) {
  const admin = role === 'ADMIN', student = role === 'STUDENT';
  const api = '/api/' + role.toLowerCase() + '/exams';
  const area = element('div'), table = element('div');
  if (admin) content.append(academicAction('Create exam', () => edit()));
  content.append(element('p', 'Exam times are shown in IST.'), area, table);
  async function load() {
    const [exams, backlog] = await Promise.all([request(api), student ? request('/api/student/backlog-exams') : []]);
    area.replaceChildren();
    const options = new Set(backlog.map(row => row.id));
    const rows = exams.map(row => ({...row, start_time: campusLabel(row.starts_at), end_time: campusLabel(row.ends_at),
      kind_label: row.kind === 'REGULAR' ? 'Regular' : 'Backlog', exam_status: examStates[row.state]}));
    renderTable(table, rows, role === 'FACULTY' ? null : choose, {
      fields: admin ? [...examFields, 'cohort_code', 'max_score', 'pass_score'] : examFields,
      actionLabel: row => admin ? 'Manage' : options.has(row.id) ? 'Register for backlog' : null,
      emptyMessage: 'No exams to show.'
    });
    return rows;
  }
  function choose(row) {
    if (student) {
      submitServiceRequest(area, 'BACKLOG', row.id, row.title, load);
      return;
    }
    manage(row).catch(error => { status.textContent = error.message; });
  }
  async function manage(row) {
    const heading = element('h4', row.title);
    const controls = element('div');
    controls.className = 'academic-toolbar';
    area.replaceChildren(heading, controls);
    if (['DRAFT', 'SCHEDULED'].includes(row.state)) controls.append(academicAction('Edit exam', () => edit(row)));
    if (row.state === 'CANCELLED' || row.state === 'DRAFT') {
      revealEditor(area);
      return;
    }
    const roster = await request(api + '/' + row.id + '/roster');
    if (!area.contains(controls)) return;
    const marks = element('div'), scoreEditor = element('div');
    const complete = roster.length > 0 && roster.every(entry => entry.eligible && entry.score != null);
    const count = roster.filter(entry => entry.eligible && entry.score != null).length;
    area.append(element('p', count + ' marked of ' + roster.filter(entry => entry.eligible).length + ' eligible students'),
      scoreEditor, marks);
    if (row.state === 'SCHEDULED' && complete) {
      controls.append(academicAction('Publish results', async () => {
        if (!window.confirm('Publish these results to the students on this register?')) return;
        await mutate(api + '/' + row.id + '/release', {revision: row.revision}, 'POST');
        await load();
        status.textContent = 'Results published.';
      }));
    }
    renderTable(marks, roster, row.state === 'SCHEDULED' ? entry => editScore(row, entry, scoreEditor) : null, {
      fields: ['student', 'score', 'eligible'],
      actionLabel: entry => entry.eligible ? 'Enter score' : 'Review draft score',
      emptyMessage: 'No eligible students. Check enrollment or approved backlog registrations.'
    });
    revealEditor(area);
  }
  async function edit(row = {}) {
    const offerings = await request('/api/admin/setup/offerings');
    const form = academicEditor(area, row.id ? 'Edit exam' : 'New exam',
      ['title', 'offering_code', 'kind', 'room', 'start_time', 'end_time', 'max_score', 'pass_score', 'state'],
      {max_score: 100, pass_score: 40, ...row}, async values => {
        await mutate(api + '/' + id, {
          title: values.title, offeringCode: values.offering_code, kind: values.kind, room: values.room,
          startsAt: values.start_time + ':00+05:30', endsAt: values.end_time + ':00+05:30',
          maxScore: Number(values.max_score), passScore: Number(values.pass_score),
          state: values.state, revision: row.revision ?? null
        });
        await load();
      });
    const id = row.id || crypto.randomUUID();
    replaceWithSelect(form, 'offering_code',
      offerings.map(item => academicOption(item.code, item.name + ' · ' + item.cohort_code + ' · ' + item.term_code)),
      row.offering_code);
    replaceWithSelect(form, 'kind', [academicOption('REGULAR', 'Regular'), academicOption('BACKLOG', 'Backlog')],
      row.kind || 'REGULAR');
    const states = row.id ? ['DRAFT', 'SCHEDULED', 'CANCELLED'] : ['DRAFT', 'SCHEDULED'];
    replaceWithSelect(form, 'state', states.map(value => academicOption(value, examStates[value])), row.state || 'DRAFT');
    for (const field of ['start_time', 'end_time']) {
      const input = form.elements[field];
      input.type = 'datetime-local';
      input.value = campusInput(field === 'start_time' ? row.starts_at : row.ends_at);
      input.closest('label').firstChild.textContent = field === 'start_time' ? 'Starts (IST)' : 'Ends (IST)';
    }
    for (const field of ['max_score', 'pass_score']) {
      const input = form.elements[field];
      input.type = 'number';
      input.min = '0.01';
      input.max = '99999.99';
      input.step = '0.01';
    }
    form.elements.title.maxLength = 140;
    form.elements.room.maxLength = 80;
    revealEditor(area);
  }
  function editScore(exam, entry, scoreEditor) {
    const url = api + '/' + exam.id + '/scores/' + entry.student_id;
    if (entry.eligible) {
      const form = academicEditor(scoreEditor, 'Score for ' + entry.student, ['score'], entry, async values => {
        await mutate(url, {score: Number(values.score), examRevision: exam.revision, revision: entry.revision ?? null});
        await refreshRegister(exam.id);
      });
      configureScore(form.elements.score, exam.max_score, true);
    } else {
      scoreEditor.replaceChildren(element('p', entry.student + ' is outside the current exam roster.'));
    }
    if (entry.score != null) {
      scoreEditor.append(academicAction('Discard draft score', async () => {
        if (!window.confirm('Discard this draft score?')) return;
        await mutate(url, {revision: entry.revision}, 'DELETE');
        await refreshRegister(exam.id);
        status.textContent = 'Draft score discarded.';
      }));
    }
    revealEditor(scoreEditor);
  }
  async function refreshRegister(id) {
    const rows = await load();
    const exam = rows.find(row => row.id === id);
    if (exam) await manage(exam);
  }
  return load;
}

function configureScore(input, maximum, required) {
  input.type = 'number';
  input.min = '0';
  input.max = maximum;
  input.step = '0.01';
  input.required = required;
}

function resultsPanel(content) {
  const area = element('div'), table = element('div');
  content.append(area, table);
  async function load() {
    const [results, requests] = await Promise.all([request('/api/student/results'), request('/api/student/services/requests')]);
    const pending = new Set(requests.filter(row => ['SUBMITTED', 'IN_REVIEW'].includes(row.state)).map(row => row.exam_id));
    area.replaceChildren();
    renderTable(table, results.map(row => ({...row, published: campusLabel(row.released_at),
      last_updated: campusLabel(row.updated_at), outcome: row.outcome === 'PASS' ? 'Pass' : 'Fail'})), choose, {
      fields: ['title', 'course', 'score', 'max_score', 'pass_score', 'outcome', 'published', 'last_updated'],
      actionLabel: row => pending.has(row.exam_id) ? null : 'Request review',
      emptyMessage: 'No results have been published for you yet.'
    });
  }
  function choose(row) {
    submitServiceRequest(area, 'REVALUATION', row.exam_id, row.title, load, ['REVALUATION', 'RESULT_CORRECTION']);
  }
  return load;
}

function submitServiceRequest(area, kind, examId, title, refresh, kinds = [kind]) {
  const id = crypto.randomUUID();
  const editable = kinds.length > 1;
  const form = academicEditor(area, title, [...(editable ? ['service'] : []), 'summary'], {service: kind},
    async values => {
      await mutate('/api/student/services/requests/' + id,
        {kind: editable ? values.service : kind, examId, details: values.summary});
      await refresh();
      status.textContent = 'Request submitted. Follow its status in Student requests.';
    });
  if (editable) {
    replaceWithSelect(form, 'service', kinds.map(value => academicOption(value, serviceKinds[value])), kind);
  }
  form.elements.summary.maxLength = 1000;
  form.elements.summary.closest('label').firstChild.textContent = 'Request details';
  form.querySelector('button').textContent = 'Submit request';
  if (kind === 'ACCOUNT_SUPPORT' || kinds.includes('ACCOUNT_SUPPORT')) {
    form.before(element('p', 'Keep passwords and verification codes private.'));
  }
  revealEditor(area);
}

function serviceRequestsPanel(content, admin) {
  const api = admin ? '/api/admin/student-services/requests' : '/api/student/services/requests';
  const area = element('div'), table = element('div');
  if (!admin) content.append(academicAction('New service request',
    () => submitServiceRequest(area, 'BONA_FIDE', null, 'New student request', load, generalServices)));
  content.append(area, table);
  async function load() {
    const rows = await request(api);
    area.replaceChildren();
    renderTable(table, rows.map(row => ({...row, request_type: serviceKinds[row.kind],
      request_status: serviceStates[row.state], received: campusLabel(row.created_at), reviewed: campusLabel(row.reviewed_at)})),
      openCase, {
        fields: [...(admin ? ['student'] : []), 'request_type', 'exam', 'request_status', 'office', 'received', 'reviewed'],
        actionLabel: row => admin && nextStatuses(row).length ? 'Review' : 'View',
        emptyMessage: 'No student requests yet.'
      });
  }
  function nextStatuses(row) {
    if (row.state === 'SUBMITTED') return ['IN_REVIEW', 'APPROVED', 'REJECTED'];
    if (row.state === 'IN_REVIEW') return ['APPROVED', 'REJECTED'];
    if (row.state === 'APPROVED' && generalServices.includes(row.kind)) return ['COMPLETED'];
    return [];
  }
  function openCase(row) {
    area.replaceChildren(element('h4', serviceKinds[row.kind]), element('p', serviceStates[row.state]),
      element('p', row.details));
    if (row.response) area.append(element('h5', 'Office response'), element('p', row.response));
    if (row.old_score != null) {
      area.append(element('p', row.new_score == null ? 'Score confirmed at ' + row.old_score
        : 'Score adjusted from ' + row.old_score + ' to ' + row.new_score));
    }
    area.append(officeCard(row, false));
    const next = nextStatuses(row);
    if (admin && next.length) {
      const isResult = ['REVALUATION', 'RESULT_CORRECTION'].includes(row.kind);
      const review = element('div');
      area.append(review);
      const form = academicEditor(review, 'Review request', ['decision', 'summary', ...(isResult ? ['score'] : [])],
        {decision: next[0]}, async values => {
          await mutate(api + '/' + row.id, {state: values.decision, response: values.summary, revision: row.revision,
            score: isResult && values.decision === 'APPROVED' && values.score !== '' ? Number(values.score) : null});
          await load();
        });
      replaceWithSelect(form, 'decision', next.map(value => academicOption(value, serviceStates[value])), next[0]);
      form.elements.summary.maxLength = 1000;
      form.elements.summary.closest('label').firstChild.textContent = 'Office response';
      if (isResult) {
        const input = form.elements.score;
        const label = input.closest('label');
        label.firstChild.textContent = 'New score';
        function toggleScore() {
          label.hidden = form.elements.decision.value !== 'APPROVED';
          configureScore(input, row.max_score, !label.hidden && row.kind === 'RESULT_CORRECTION');
        }
        form.elements.decision.addEventListener('change', toggleScore);
        toggleScore();
      }
    }
    revealEditor(area);
  }
  return load;
}

function officeCard(row, includeTopic) {
  const article = element('article');
  article.className = 'inbox-notice';
  if (includeTopic) article.append(element('h4', serviceKinds[row.kind]));
  article.append(element('strong', row.office), element('p', row.instructions));
  if (row.email) {
    const contact = element('a', row.email);
    contact.href = 'mailto:' + encodeURIComponent(row.email).replace('%40', '@');
    article.append(contact);
  }
  return article;
}

function serviceOfficesPanel(content, admin) {
  const api = admin ? '/api/admin/student-services/offices' : '/api/student/services/offices';
  const area = element('div'), offices = element('div');
  content.append(area, offices);
  async function load() {
    const rows = await request(api);
    area.replaceChildren();
    offices.replaceChildren(...rows.map(row => {
      const article = officeCard(row, true);
      if (admin) article.append(academicAction('Edit guidance', () => edit(row)));
      return article;
    }));
  }
  function edit(row) {
    const form = academicEditor(area, serviceKinds[row.kind], ['office', 'email', 'summary'],
      {...row, summary: row.instructions}, async values => {
        await mutate(api + '/' + row.kind, {
          office: values.office, email: values.email || null, instructions: values.summary, revision: row.revision
        });
        await load();
      });
    form.elements.email.type = 'email';
    form.elements.email.required = false;
    form.elements.email.maxLength = 254;
    form.elements.office.maxLength = 120;
    form.elements.summary.maxLength = 1000;
    form.elements.summary.closest('label').firstChild.textContent = 'Instructions';
    revealEditor(area);
  }
  return load;
}
