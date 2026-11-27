const admissionDialog = document.querySelector('#admission-dialog');
const departmentDialog = document.querySelector('#department-dialog');
document.querySelectorAll('[data-admission]').forEach((button) => button.addEventListener('click', () => admissionDialog.showModal()));
document.querySelectorAll('#admission-dialog .dialog-close').forEach((button) => button.addEventListener('click', () => admissionDialog.close()));
document.querySelector('#admission-notices')?.addEventListener('click', () => admissionDialog.close());
admissionDialog.addEventListener('click', (event) => { if (event.target === admissionDialog) admissionDialog.close(); });
document.querySelectorAll('#department-dialog .dialog-close').forEach((button) => button.addEventListener('click', () => departmentDialog.close()));
departmentDialog.addEventListener('click', (event) => { if (event.target === departmentDialog) departmentDialog.close(); });

const helpPanel = document.querySelector('#help-panel');
const helpToggle = document.querySelector('#help-toggle');
const helpAnswer = document.querySelector('#help-answer');
const helpReplies = {
  admissions:'Use the Admission enquiry button. A verified office phone will be added before launch.',
  scholarships:'Scholarship announcements will appear in the public Campus Desk. Verified office contacts will be added before launch.',
  programmes:'Browse the BTech carousel and select a branch for its concept snapshot and proposed honours/minors.',
  student:'Sign in to the student portal to view your academic profile. Certificate requests are planned for Release 4.'
};
helpToggle.addEventListener('click', () => { helpPanel.hidden = !helpPanel.hidden; helpToggle.setAttribute('aria-expanded', String(!helpPanel.hidden)); });
document.querySelector('#help-close').addEventListener('click', () => { helpPanel.hidden = true; helpToggle.setAttribute('aria-expanded','false'); });
document.querySelectorAll('[data-help]').forEach((button) => button.addEventListener('click', () => {
  helpAnswer.textContent = helpReplies[button.dataset.help];
  helpAnswer.hidden = false;
}));
const pathways = {
  cse: {title:'Computer Science & Engineering', description:'Computing foundations, software systems, and collaborative product projects.', image:'images/student-project-lab.jpg', honors:['Artificial intelligence and machine learning','Data science and analytics','Full-stack software development'], minors:['Cybersecurity foundations','Internet of Things','Cloud and distributed systems']},
  ct: {title:'Computer Technology', description:'Software platforms, data systems, and practical product engineering.', image:'images/student-project-lab.jpg', honors:['AI and machine learning','Full-stack development','Big data systems'], minors:['Cybersecurity','Cloud computing','Internet of Things']},
  it: {title:'Information Technology', description:'Networks, digital services, security, and dependable information systems.', image:'images/library-commons.jpg', honors:['Data science and machine learning','Cybersecurity','Application engineering'], minors:['Business analytics','Cloud infrastructure','Internet of Things']},
  aiml: {title:'Artificial Intelligence & Machine Learning', description:'Machine learning, intelligent applications, and responsible AI practice.', image:'images/engineering-lab.jpg', honors:['Deep learning','Computer vision','Natural language processing'], minors:['Robotics','Embedded systems','Data engineering']},
  aids: {title:'Artificial Intelligence & Data Science', description:'Data modelling, analytics, and evidence-led decision systems.', image:'images/engineering-lab.jpg', honors:['Data science and machine learning','Big data analytics','Applied AI'], minors:['Geospatial analytics','Fintech analytics','Healthcare data systems']},
  ece: {title:'Electronics & Communication', description:'Circuits, connected devices, communication systems, and embedded design.', image:'images/engineering-lab.jpg', honors:['VLSI design','Wireless communication','Computer vision and automation'], minors:['AI and machine learning','Cybersecurity','Data science']},
  eee: {title:'Electrical & Electronics', description:'Energy systems, electrical machines, control, and automation.', image:'images/engineering-lab.jpg', honors:['Renewable energy systems','Microgrid technology','Industrial automation'], minors:['Electric vehicles','Internet of Things','Embedded systems']},
  me: {title:'Mechanical Engineering', description:'Product design, manufacturing, materials, and mobility systems.', image:'images/engineering-lab.jpg', honors:['Advanced industrial robotics','Additive manufacturing','Thermal and energy systems'], minors:['Mechatronics','Electric vehicles','Computer-aided engineering']},
  ce: {title:'Civil Engineering', description:'Structures, infrastructure, geospatial tools, and sustainable built environments.', image:'images/campus-life.jpg', honors:['Construction technology','Remote sensing and GIS','Sustainable infrastructure'], minors:['Structural design','Water resources','Project management']},
  chem: {title:'Chemical Engineering', description:'Process design, materials, energy, and resource-conscious production.', image:'images/engineering-lab.jpg', honors:['Process intensification','Materials engineering','Clean energy processes'], minors:['Environmental engineering','Industrial safety','Process data analytics']},
  robotics: {title:'Robotics & Automation', description:'Robotic systems, sensing, motion control, and industrial automation.', image:'images/engineering-lab.jpg', honors:['Robotic systems','Autonomous mobile robots','Machine vision'], minors:['Control systems','Embedded computing','Artificial intelligence']},
  design: {title:'Computer Science & Design', description:'Human-centred computing, interaction, and digital product design.', image:'images/library-commons.jpg', honors:['Interaction design','Visual computing','Digital product engineering'], minors:['Artificial intelligence','Product management','Digital media']}
};

function fillList(elementId, values) {
  const list = document.getElementById(elementId);
  list.replaceChildren(...values.map((value) => { const item = document.createElement('li'); item.textContent = value; return item; }));
}

document.querySelectorAll('[data-department]').forEach((card) => card.addEventListener('click', () => {
  const item = pathways[card.dataset.department];
  document.querySelector('#department-title').textContent = item.title;
  document.querySelector('#department-description').textContent = item.description;
  document.querySelector('#department-image').src = item.image;
  document.querySelector('#department-image').alt = `Generated concept image related to ${item.title}`;
  fillList('honours-list', item.honors);
  fillList('minors-list', item.minors);
  departmentDialog.showModal();
}));

const carousel = document.querySelector('#program-carousel');
const departmentCards = [...carousel.querySelectorAll('.program-card')];
const carouselInner = document.createElement('div');
carouselInner.className = 'carousel-inner';
departmentCards.forEach((card, index) => {
  if (index % 4 === 0) {
    const slide = document.createElement('div');
    slide.className = `carousel-item${index === 0 ? ' active' : ''}`;
    const grid = document.createElement('div');
    grid.className = 'program-slide-grid row g-3';
    slide.append(grid);
    carouselInner.append(slide);
  }
  const currentSlide = carouselInner.lastElementChild.querySelector('.program-slide-grid');
  const cardWrap = document.createElement('div');
  cardWrap.className = 'program-slide-card col';
  cardWrap.append(card);
  currentSlide.append(cardWrap);
});
carousel.replaceChildren(carouselInner);
const carouselApi = window.bootstrap?.Carousel.getOrCreateInstance(carousel, {interval:false,wrap:true,touch:true});
let slides = [...carousel.querySelectorAll('.carousel-item')];
let activeSlide = 0;
let wheelDelta = 0;
let lastWheelMove = 0;
carousel.addEventListener('slid.bs.carousel', (event) => { activeSlide = event.to; });
const moveCarousel = (direction) => {
  if (!slides.length) return;
  if (carouselApi) direction > 0 ? carouselApi.next() : carouselApi.prev();
  else {
    slides[activeSlide].classList.remove('active');
    activeSlide = (activeSlide + direction + slides.length) % slides.length;
    slides[activeSlide].classList.add('active');
  }
};
document.querySelector('#program-prev').addEventListener('click', () => moveCarousel(-1));
document.querySelector('#program-next').addEventListener('click', () => moveCarousel(1));
carousel.addEventListener('wheel', (event) => {
  if (Math.abs(event.deltaY) > Math.abs(event.deltaX)) {
    event.preventDefault();
    wheelDelta += event.deltaY;
    const now = Date.now();
    if (Math.abs(wheelDelta) >= 180 && now - lastWheelMove >= 850) {
      moveCarousel(wheelDelta > 0 ? 1 : -1);
      wheelDelta = 0;
      lastWheelMove = now;
    }
  }
}, {passive:false});

document.querySelector('#mobile-menu').addEventListener('click', () => document.querySelector('#main-nav').classList.toggle('nav-open'));
document.querySelectorAll('#main-nav a').forEach((link) => link.addEventListener('click', (event) => {
  const parentItem = link.parentElement;
  const isTopLevelMenu = parentItem.classList.contains('nav-item') && parentItem.classList.contains('has-menu');
  if (isTopLevelMenu && window.matchMedia('(max-width: 760px)').matches) {
    event.preventDefault();
    parentItem.classList.toggle('mobile-open');
  } else {
    document.querySelector('#main-nav').classList.remove('nav-open');
    document.querySelectorAll('.nav-item.mobile-open').forEach((item) => item.classList.remove('mobile-open'));
  }
}));

const revealItems = document.querySelectorAll('main section, .site-footer');
revealItems.forEach((item) => item.classList.add('reveal'));
if ('IntersectionObserver' in window) {
  const observer = new IntersectionObserver((entries, currentObserver) => entries.forEach((entry) => {
    if (entry.isIntersecting) { entry.target.classList.add('is-visible'); currentObserver.unobserve(entry.target); }
  }), {threshold:0.1});
  revealItems.forEach((item) => observer.observe(item));
} else revealItems.forEach((item) => item.classList.add('is-visible'));

async function requestJson(url, options) {
  const response = await fetch(url, options);
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.detail || 'The request could not be completed.');
  return payload;
}

async function loadPublicAnnouncements() {
  const track = document.querySelector('#announcement-track');
  if (location.protocol === 'file:') return;
  track.replaceChildren();
  try {
    const items = await requestJson('/api/public/announcements');
    track.replaceChildren(...items.map((notice) => {
      const card = document.createElement('article');
      card.className = 'announcement-card';
      const tag = document.createElement('span');
      tag.className = `announcement-tag ${notice.category === 'ADMISSIONS' ? 'tag-orange' : notice.category === 'SCHOLARSHIPS' ? 'tag-blue' : notice.category === 'CAMPUS EVENT' ? 'tag-green' : 'tag-violet'}`;
      tag.textContent = notice.category;
      const title = document.createElement('h3');
      title.textContent = notice.title;
      const summary = document.createElement('p');
      summary.textContent = notice.summary;
      card.append(tag, title, summary);
      if (notice.actionLabel) {
        const action = document.createElement('a');
        action.textContent = notice.actionLabel;
        action.href = typeof notice.actionUrl === 'string' && notice.actionUrl.startsWith('#') ? notice.actionUrl : '#announcements';
        card.append(action);
      }
      return card;
    }));
  } catch {
    track.textContent = 'Announcements are temporarily unavailable.';
  }
}

async function loadCollegeOverview() {
  try {
    const profile = await requestJson('/api/public/overview');
    Object.entries(profile).forEach(([key, value]) => {
      const metric = document.querySelector(`[data-metric="${key}"]`);
      if (metric && Number.isFinite(Number(value))) metric.textContent = Number(value).toLocaleString('en-IN');
    });
  } catch {
    // Keep the static demo values as a graceful offline preview.
  }
}

async function loadDepartmentNames() {
  if (location.protocol === 'file:') return;
  carouselInner.replaceChildren();
  slides = [];
  try {
    const departments = await requestJson('/api/public/departments');
    const cards = departments.map((department, index) => {
      let card = departmentCards.find(item => item.dataset.department === department.code);
      if (!card) {
        card = document.createElement('article');
        card.className = 'program-card';
        const code = document.createElement('span');
        code.className = 'program-code';
        card.append(code, document.createElement('h3'));
      }
      const code = card.querySelector('.program-code');
      const title = card.querySelector('h3');
      if (title) title.textContent = department.name;
      if (Object.hasOwn(pathways, department.code)) pathways[department.code].title = department.name;
      if (code) code.textContent = `${String(index + 1).padStart(2, '0')} · ${department.code.toUpperCase()}`;
      return card;
    });
    carouselInner.replaceChildren();
    cards.forEach((card, index) => {
      if (index % 4 === 0) {
        const slide = document.createElement('div');
        slide.className = 'carousel-item' + (index === 0 ? ' active' : '');
        const grid = document.createElement('div');
        grid.className = 'program-slide-grid row g-3';
        slide.append(grid);
        carouselInner.append(slide);
      }
      const wrapper = document.createElement('div');
      wrapper.className = 'program-slide-card col';
      wrapper.append(card);
      carouselInner.lastElementChild.firstElementChild.append(wrapper);
    });
    slides = [...carousel.querySelectorAll('.carousel-item')];
    activeSlide = 0;
  } catch {
    if (location.protocol !== 'file:') {
      carouselInner.textContent = 'Departments are temporarily unavailable.';
      slides = [];
    }
  }
}

const enquiryForm = document.querySelector('#enquiry-form');
enquiryForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const status = document.querySelector('#enquiry-status');
  const submit = enquiryForm.querySelector('[type="submit"]');
  const values = Object.fromEntries(new FormData(enquiryForm).entries());
  values.privacyConsent = enquiryForm.elements.privacyConsent.checked;
  status.textContent = 'Sending your enquiry…';
  submit.disabled = true;
  try {
    const receipt = await requestJson('/api/public/enquiries', {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify(values)
    });
    status.textContent = `${receipt.message} Reference: ${receipt.reference}. Received: ${receipt.receivedAtDisplay}`;
    enquiryForm.reset();
  } catch (error) {
    status.textContent = error.message === 'Failed to fetch'
      ? 'The enquiry service is unavailable. Start the Vishvaya application and try again.'
      : error.message;
  } finally {
    submit.disabled = false;
  }
});

loadPublicAnnouncements();
loadCollegeOverview();
loadDepartmentNames();
