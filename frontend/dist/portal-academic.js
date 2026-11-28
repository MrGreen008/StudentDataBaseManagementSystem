const campusZone = 'Asia/Kolkata';
const campusDate = new Intl.DateTimeFormat('sv-SE', {timeZone: campusZone, year: 'numeric', month: '2-digit', day: '2-digit'});
const campusDateTime = new Intl.DateTimeFormat('sv-SE', {
  timeZone: campusZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
});
const campusDisplay = new Intl.DateTimeFormat('en-IN', {timeZone: campusZone, dateStyle: 'medium', timeStyle: 'short'});
const academicMarkChoices = [{value: 'PRESENT', label: 'Present'}, {value: 'ABSENT', label: 'Absent'}];

function academicDateOffset(date, days) {
  const day = new Date(date + 'T00:00:00Z');
  day.setUTCDate(day.getUTCDate() + days);
  return day.toISOString().slice(0, 10);
}
function currentCampusWeek() {
  const today = campusDate.format(new Date());
  const dayOfWeek = new Date(today + 'T00:00:00Z').getUTCDay();
  return academicDateOffset(today, -((dayOfWeek + 6) % 7));
}
function campusInput(value) {
  return value == null ? '' : campusDateTime.format(new Date(value)).replace(' ', 'T');
}
function campusLabel(value) {
  return value == null ? '—' : campusDisplay.format(new Date(value));
}
function academicOption(value, label) {
  return {value, label};
}
function academicAction(label, handler) {
  const button = element('button', label);
  button.type = 'button';
  button.addEventListener('click', async () => {
    button.disabled = true;
    try {
      await handler();
    } catch (error) {
      status.textContent = error.message;
    } finally {
      button.disabled = false;
    }
  });
  return button;
}
function academicSection(dashboard, title, setup) {
  const section = element('section');
  section.append(element('h3', title));
  const content = section.appendChild(element('div'));
  section.refresh = setup(content);
  dashboard.append(section);
}
function academicEditor(area, title, fields, values, save) {
  const form = editor(fields, values, save);
  const close = academicAction('Close', () => area.replaceChildren());
  area.replaceChildren(element('h4', title), form, close);
  return form;
}
function academicPanels(dashboard, account) {
  const api = '/api/' + account.role.toLowerCase() + '/academic';
  if (account.role !== 'FACULTY') {
    academicSection(dashboard, 'Enrollments', content => enrollmentPanel(content, api, account.role === 'ADMIN'));
  }
  academicSection(dashboard, 'Timetable', content => timetablePanel(content, api, account.role));
  academicSection(dashboard, 'Attendance', content => attendancePanel(content, api, account.role));
  if (account.role !== 'STUDENT') {
    academicSection(dashboard, 'Faculty availability', content => availabilityPanel(content, api, account));
  }
  academicSection(dashboard, 'Academic bulletins', content => async () => {
    const inbox = await request('/api/portal/notices');
    const bulletins = inbox.filter(row => row.topic && row.topic !== 'GENERAL');
    content.replaceChildren(...bulletins.map(noticeCard));
    if (!bulletins.length) content.append(element('p', 'No academic bulletins for your account.'));
  });
}
function enrollmentPanel(content, api, admin) {
  const area = element('div'), table = element('div');
  if (admin) content.append(academicAction('Enroll a student', () => edit()));
  content.append(area, table);
  async function load() {
    const rows = await request(api + '/enrollments');
    area.replaceChildren();
    renderTable(table, rows, admin ? row => edit(row).catch(error => { status.textContent = error.message; }) : null, {
      fields: [...(admin ? ['student'] : []), 'course', 'offering_code', 'term_code', 'starts_on', 'ends_on'],
      emptyMessage: admin ? 'No enrollments yet.' : 'No courses have been assigned to your academic profile.'
    });
  }
  async function edit(row = {}) {
    const [students, offerings, terms] = await Promise.all([
      request('/api/admin/setup/students'), request('/api/admin/setup/offerings'), request('/api/admin/setup/terms')
    ]);
    const form = academicEditor(area, row.student_id ? 'Edit enrollment' : 'New enrollment',
      ['student_id', 'offering_code', 'starts_on', 'ends_on'], row, async values => {
        const url = api + '/enrollments/' + encodeURIComponent(values.offering_code) + '/' + values.student_id;
        await mutate(url, {startsOn: values.starts_on, endsOn: values.ends_on, revision: row.revision ?? null});
        await load();
      });
    replaceWithSelect(form, 'student_id', students.filter(item => item.active)
      .map(item => academicOption(item.account_id, item.name + ' · ' + item.cohort_code)), row.student_id);
    function setOfferings() {
      const student = students.find(item => item.account_id === form.elements.student_id.value);
      const choices = offerings.filter(item => item.active && item.cohort_code === student?.cohort_code
          && terms.some(term => term.active && item.term_code === term.code))
        .map(item => academicOption(item.code, item.name + ' · ' + item.term_code));
      replaceWithSelect(form, 'offering_code', choices, row.offering_code);
      form.elements.offering_code.addEventListener('change', () => {
        const offering = offerings.find(item => item.code === form.elements.offering_code.value);
        const term = terms.find(item => item.code === offering?.term_code);
        form.elements.starts_on.value = term?.starts_on?.slice(0, 10) || '';
        form.elements.ends_on.value = term?.ends_on?.slice(0, 10) || '';
      });
    }
    form.elements.student_id.addEventListener('change', setOfferings);
    setOfferings();
    if (row.student_id) {
      for (const field of ['student_id', 'offering_code']) form.elements[field].disabled = true;
    }
    revealEditor(area);
  }
  return load;
}
function timetablePanel(content, api, role) {
  let week = currentCampusWeek(), requestNumber = 0;
  const admin = role === 'ADMIN', faculty = role === 'FACULTY';
  const toolbar = element('div'), range = element('p'), area = element('div'), table = element('div');
  toolbar.className = 'academic-toolbar';
  range.setAttribute('aria-live', 'polite');
  toolbar.append(academicAction('Previous week', () => step(-7)),
    academicAction('This week', () => { week = currentCampusWeek(); return load(); }),
    academicAction('Next week', () => step(7)));
  if (admin) toolbar.append(academicAction('Schedule class', () => edit()));
  content.append(toolbar, range, area, table);
  function step(days) {
    week = academicDateOffset(week, days);
    return load();
  }
  async function load() {
    const number = ++requestNumber;
    area.replaceChildren();
    table.replaceChildren(element('p', 'Loading timetable…'));
    range.textContent = week + ' to ' + academicDateOffset(week, 6) + ' · Campus time (IST)';
    const rows = await request(api + '/timetable?week=' + week);
    if (number !== requestNumber) return;
    const states = {SCHEDULED: 'Awaiting attendance', SUBMITTED: 'Attendance submitted', CANCELLED: 'Cancelled'};
    const availability = {LEAVE: 'On leave', AVAILABLE: 'Available today', UNAVAILABLE: 'Unavailable'};
    const display = rows.map(row => ({...row, starts: campusLabel(row.starts_at), ends: campusLabel(row.ends_at),
      attendance: states[row.state], faculty_availability: availability[row.availability] || '—',
      availability_updated: campusLabel(row.availability_updated_at)}));
    const choose = admin || faculty ? row => {
      const open = admin && row.state !== 'SUBMITTED' ? edit : register;
      open(row).catch(error => { status.textContent = error.message; });
    } : null;
    renderTable(table, display, choose, {
      fields: ['course', 'cohort_code', 'starts', 'ends', 'room', 'instructor', 'attendance',
        'faculty_availability', 'availability_updated'],
      actionLabel: row => admin && row.state !== 'SUBMITTED' ? 'Edit'
        : row.state === 'CANCELLED' ? null : row.state === 'SUBMITTED' ? 'View register' : 'Open register',
      emptyMessage: 'No classes scheduled for this week.'
    });
  }
  async function edit(row = {}) {
    const choices = await request(api + '/teaching-choices');
    if (!choices.length) {
      area.replaceChildren(element('p', 'Create an active faculty assignment in College Setup before scheduling.'));
      return;
    }
    const form = academicEditor(area, row.id ? 'Edit class' : 'New class',
      ['assignment', 'room', 'start_time', 'end_time'],
      {room: row.room, start_time: campusInput(row.starts_at), end_time: campusInput(row.ends_at)}, async values => {
        const [offeringCode, instructorId] = values.assignment.split('/');
        await mutate(api + '/sessions/' + id, {
          offeringCode, instructorId, room: values.room,
          startsAt: values.start_time + ':00+05:30', endsAt: values.end_time + ':00+05:30',
          revision: row.revision ?? null
        });
        const lesson = values.start_time.slice(0, 10);
        week = academicDateOffset(lesson, -((new Date(lesson + 'T00:00:00Z').getUTCDay() + 6) % 7));
        await load();
      });
    const id = row.id || crypto.randomUUID();
    replaceWithSelect(form, 'assignment', choices.map(item =>
      academicOption(item.offering_code + '/' + item.instructor_id, item.course + ' · ' + item.offering_code + ' · ' + item.instructor)),
      row.id ? row.offering_code + '/' + row.instructor_id : '');
    for (const key of ['start_time', 'end_time']) {
      const input = form.elements[key];
      input.type = 'datetime-local';
      input.value = campusInput(key === 'start_time' ? row.starts_at : row.ends_at);
      input.closest('label').firstChild.textContent = (key === 'start_time' ? 'Starts' : 'Ends') + ' (IST)';
    }
    if (row.state === 'SCHEDULED') {
      const cancel = academicAction('Cancel class', async () => {
        if (!window.confirm('Cancel this scheduled class?')) return;
        await mutate(api + '/sessions/' + id + '/cancel', {revision: row.revision}, 'POST');
        await load();
        status.textContent = 'Class cancelled.';
      });
      area.append(cancel);
    }
    revealEditor(area);
  }
  async function register(row) {
    const url = api + '/sessions/' + row.id + '/register';
    const register = await request(url);
    area.replaceChildren(element('h4', row.course + ' register'), element('p', campusLabel(row.starts_at)));
    const close = academicAction('Close register', () => area.replaceChildren());
    if (register.state === 'SUBMITTED') {
      const result = area.appendChild(element('div'));
      renderTable(result, register.students, null, {fields: ['student', 'mark']});
    } else if (register.state === 'CANCELLED') {
      area.append(element('p', 'This class has been cancelled.'));
    } else if (!register.started || !register.students.length) {
      area.append(element('p', !register.started ? 'Attendance opens when the class starts.' : 'No students enrolled for this class date.'));
    } else {
      const form = element('form');
      for (const student of register.students) {
        const label = element('label', student.student), input = element('input');
        input.name = student.student_id;
        label.append(input);
        form.append(label);
        replaceWithSelect(form, input.name, academicMarkChoices, '');
      }
      const save = element('button', 'Submit attendance');
      save.type = 'submit';
      form.append(save);
      form.addEventListener('submit', event => {
        event.preventDefault();
        submit(form, async () => {
          await mutate(url, {revision: register.revision, marks: Object.fromEntries(new FormData(form))});
          await load();
          status.textContent = 'Attendance submitted.';
        });
      });
      area.append(form);
    }
    area.append(close);
    revealEditor(area);
  }
  return load;
}

function attendancePanel(content, api, role) {
  const student = role === 'STUDENT';
  const marks = element('div'), reports = element('div'), area = element('div');
  if (student) content.append(marks);
  else content.append(element('p', 'Open a class in Timetable to view its register.'));
  content.append(element('h4', student ? 'My discrepancy reports' : 'Discrepancy reports'), reports, area);
  async function load() {
    const [history, cases] = await Promise.all([
      student ? request(api + '/attendance') : [], request(api + '/reports')
    ]);
    area.replaceChildren();
    const pending = new Set(cases.filter(row => row.decision === 'OPEN').map(row => row.session_id));
    if (student) {
      renderTable(marks, history.map(row => ({...row, class_date: campusLabel(row.starts_at)})), openReport, {
        fields: ['course', 'class_date', 'mark'], actionLabel: row => pending.has(row.session_id) ? null : 'Report discrepancy',
        emptyMessage: 'No attendance has been submitted for you yet.'
      });
    }
    renderTable(reports, cases.map(row => ({...row, class_date: campusLabel(row.starts_at), reviewed: campusLabel(row.reviewed_at)})),
      student ? null : decide, {
        fields: [...(student ? [] : ['student']), 'course', 'class_date', 'explanation', 'original_mark',
          'decision', 'corrected_mark', 'review_reason', 'reviewed'],
        actionLabel: row => row.decision === 'OPEN' ? 'Review' : null,
        emptyMessage: 'No discrepancies reported.'
      });
  }
  function openReport(row) {
    const id = crypto.randomUUID();
    const form = academicEditor(area, 'Report attendance for ' + row.course, ['summary'], {}, async values => {
      await mutate(api + '/sessions/' + row.session_id + '/reports/' + id,
        {explanation: values.summary, markRevision: row.revision});
      await load();
    });
    form.elements.summary.maxLength = 1000;
    form.elements.summary.closest('label').firstChild.textContent = 'Why should this mark be reviewed?';
    revealEditor(area);
  }
  function decide(row) {
    const form = academicEditor(area, 'Review ' + row.student + '’s attendance',
      ['decision', 'corrected_mark', 'summary'], {decision: 'ACCEPTED'}, async values => {
        const accepted = values.decision === 'ACCEPTED';
        await mutate(api + '/reports/' + row.id + '/decision', {
          revision: row.revision, accepted, correctedMark: accepted ? values.corrected_mark : null, reason: values.summary
        });
        await load();
      });
    form.before(element('p', row.explanation));
    form.elements.summary.maxLength = 1000;
    form.elements.summary.closest('label').firstChild.textContent = 'Reason for decision';
    replaceWithSelect(form, 'decision', [academicOption('ACCEPTED', 'Accept and correct'), academicOption('REJECTED', 'Reject')], 'ACCEPTED');
    replaceWithSelect(form, 'corrected_mark', academicMarkChoices.filter(choice => choice.value !== row.original_mark), '');
    form.elements.decision.addEventListener('change', () => {
      const corrected = form.elements.corrected_mark;
      corrected.required = form.elements.decision.value === 'ACCEPTED';
      corrected.closest('label').hidden = !corrected.required;
    });
    revealEditor(area);
  }
  return load;
}
function availabilityPanel(content, api, account) {
  const admin = account.role === 'ADMIN';
  const area = element('div'), table = element('div'), toolbar = element('div');
  toolbar.className = 'academic-toolbar';
  toolbar.append(academicAction('Add planned leave', () => edit({}, 'LEAVE')),
    academicAction('Set today’s availability', () => edit({}, 'AVAILABLE')));
  content.append(toolbar, area, table);
  let rows = [];
  async function load() {
    rows = await request(api + '/availability');
    area.replaceChildren();
    renderTable(table, rows.map(row => ({...row, last_updated: campusLabel(row.updated_at)})), row =>
      edit(row, row.daily ? 'AVAILABLE' : 'LEAVE').catch(error => { status.textContent = error.message; }), {
        fields: [...(admin ? ['instructor'] : []), 'starts_on', 'ends_on', 'state', 'notes', 'last_updated'],
        actionLabel: row => row.state === 'WITHDRAWN' ? null
          : !row.daily || row.starts_on === campusDate.format(new Date()) ? 'Edit' : null
      });
  }
  async function edit(row, type) {
    const today = campusDate.format(new Date());
    const daily = type !== 'LEAVE';
    const faculty = admin ? await request('/api/admin/setup/faculty') : [];
    if (admin && !faculty.some(item => item.active)) {
      area.replaceChildren(element('p', 'Create an active faculty profile in College Setup first.'));
      return;
    }
    const current = row.instructor_id || (admin ? '' : account.id);
    const values = {...row, instructor_id: current, state: row.state || type,
      starts_on: row.starts_on || today, ends_on: row.ends_on || today};
    const fields = [...(admin ? ['instructor_id'] : []), ...(daily ? ['state'] : ['starts_on', 'ends_on']), 'notes'];
    const form = academicEditor(area, daily ? 'Today’s availability · ' + today : 'Planned leave', fields, values, async input => {
      const instructorId = admin ? input.instructor_id : account.id;
      const existing = row.id ? row : daily ? rows.find(item => item.daily && item.state !== 'WITHDRAWN'
        && item.instructor_id === instructorId && item.starts_on === today) : undefined;
      await save(existing?.id || id, {
        instructorId, startsOn: daily ? today : input.starts_on, endsOn: daily ? today : input.ends_on,
        state: daily ? input.state : 'LEAVE', notes: input.notes || null, revision: existing?.revision ?? null
      });
    });
    const id = crypto.randomUUID();
    async function save(key, payload) {
      await mutate(api + '/availability/' + key, payload);
      await load();
    }
    if (admin) {
      replaceWithSelect(form, 'instructor_id', faculty.filter(item => item.active)
        .map(item => academicOption(item.account_id, item.name)), current);
      if (row.id) form.elements.instructor_id.disabled = true;
    }
    form.elements.notes.required = false;
    form.elements.notes.maxLength = 500;
    form.elements.notes.closest('label').firstChild.textContent = 'Staff-only notes (optional)';
    if (daily) {
      replaceWithSelect(form, 'state', [academicOption('AVAILABLE', 'Available'), academicOption('UNAVAILABLE', 'Unavailable')], values.state);
    }
    if (row.id) {
      area.append(academicAction('Withdraw update', async () => {
        await save(row.id, {instructorId: row.instructor_id, startsOn: row.starts_on,
          endsOn: row.ends_on, state: 'WITHDRAWN', notes: row.notes, revision: row.revision});
        status.textContent = 'Availability update withdrawn.';
      }));
    }
    revealEditor(area);
  }
  return load;
}
