let feedbackTimeout;
new MutationObserver(() => {
  clearTimeout(feedbackTimeout);
  if(status.textContent) feedbackTimeout=setTimeout(()=>{status.textContent='';},8000);
}).observe(status,{childList:true,characterData:true,subtree:true});

const themeToggle=document.querySelector('#theme-toggle');
function applyTheme(dark) {
  document.documentElement.dataset.theme=dark?'dark':'light';
  themeToggle.textContent=dark?'☀':'☾';
  themeToggle.setAttribute('aria-label',dark?'Switch to light mode':'Switch to dark mode');
}
let storedTheme;
try {storedTheme=localStorage.getItem('vishvaya-theme');} catch {}
applyTheme(storedTheme?storedTheme==='dark':matchMedia('(prefers-color-scheme: dark)').matches);
themeToggle.addEventListener('click',()=>{
  const dark=document.documentElement.dataset.theme!=='dark';applyTheme(dark);
  try {localStorage.setItem('vishvaya-theme',dark?'dark':'light');} catch {}
});

const selfService=document.querySelector('#self-service');
function showSelfService(title) {
  selfService.hidden=false;selfService.replaceChildren(element('h3',title));
  const close=element('button','Back to sign in');close.type='button';
  close.addEventListener('click',()=>{selfService.hidden=true;selfService.replaceChildren();});
  selfService.append(close);
}
document.querySelector('#open-register').addEventListener('click',async()=>{
  if(document.body.dataset.portalRole==='ADMIN') return;
  showSelfService('Register for demo access');
  const role=document.body.dataset.portalRole;
  const image=element('img');image.alt='Registration CAPTCHA. Enter the characters shown.';image.className='captcha';
  image.hidden=true;
  const refresh=element('button','Get another CAPTCHA');refresh.type='button';
  const captchaFeedback=element('p');captchaFeedback.setAttribute('role','status');
  const group=element('div');group.className='captcha-control';
  let busy=false,blockedUntil=0,refreshTimer;
  function countdown() {
    clearTimeout(refreshTimer);
    if(!group.isConnected) return;
    const remaining=Math.max(0,Math.ceil((blockedUntil-Date.now())/1000));
    refresh.disabled=busy || remaining>0;
    refresh.textContent=remaining?'Try another image in '+remaining+'s':'Get another CAPTCHA';
    if(remaining) refreshTimer=setTimeout(countdown,1000);
  }
  async function newCaptcha() {
    if(busy || Date.now()<blockedUntil) return;
    busy=true;refresh.disabled=true;
    form.elements.captcha.value='';image.hidden=true;
    captchaFeedback.textContent='Loading CAPTCHA…';
    try {
      const body=await request('/api/auth/captcha');
      if(!group.isConnected) return;
      image.src=body.image;image.hidden=false;captchaFeedback.textContent='';
    } catch(error) {
      if(!group.isConnected) return;
      if(error.retryAfter) blockedUntil=Date.now()+error.retryAfter*1000;
      captchaFeedback.textContent=error.message;
    } finally {busy=false;countdown();}
  }
  refresh.addEventListener('click',()=>newCaptcha());
  const form=editor(['username','fullName','mobileNumber','email','captcha'],{},async body=>{
    if(busy || image.hidden) throw new Error('Load a CAPTCHA image before submitting.');
    try {
      await mutate('/api/auth/register',{...body,role},'POST');
      selfService.replaceChildren(element('h3','Check your email'),element('p',
        'Registration successful. Your username and temporary password have been emailed to you. Sign in above, then choose a new password.'));
      form.reset();
    } finally {
      if(form.isConnected) {
        await newCaptcha();
      }
    }
  });
  form.elements.username.pattern='[a-zA-Z0-9][a-zA-Z0-9._+\\-]{2,99}';
  form.elements.username.maxLength=100;
  form.elements.fullName.maxLength=120;
  form.elements.email.type='email';
  form.elements.mobileNumber.pattern='[0-9]{10}';form.elements.mobileNumber.maxLength=10;
  form.elements.captcha.maxLength=6;form.elements.captcha.autocomplete='off';
  const captchaLabel=form.elements.captcha.closest('label');
  group.append(image,refresh,captchaFeedback);
  captchaLabel.before(group);
  selfService.append(element('p','Use your preferred username, full name, ten-digit mobile number and email. Accounts with the same name and mobile number cannot register twice.'),form);
  await newCaptcha();
});
document.querySelector('#open-recovery').addEventListener('click',()=>{
  showSelfService('Reset your password');
  const role=document.body.dataset.portalRole;
  const recovery=editor(['identifier'],{},async body=>{
    const response=await mutate('/api/auth/forgot-password',{identifier:body.identifier,role},'POST');
    status.textContent=response.message;
    const reset=editor(['code','password'],{},async values=>{
      await mutate('/api/auth/reset-password',{identifier:body.identifier,role,...values},'POST');
      selfService.replaceChildren(element('h3','Password reset'),element('p','Sign in above with your new password.'));
    });
    reset.elements.password.type='password';reset.elements.password.minLength=12;
    reset.elements.password.autocomplete='new-password';
    reset.elements.code.pattern='[0-9]{6}';reset.elements.code.maxLength=6;reset.elements.code.autocomplete='one-time-code';
    selfService.append(element('p',response.message),
      element('p','Codes are sent only for an existing account in this portal with a recovery email. A registration that failed to send email did not create an account. Check Spam if your account qualifies.'),
      element('p','Enter the six-digit code from your email. It expires in ten minutes.'),reset);
    recovery.hidden=true;
  });
  recovery.elements.identifier.placeholder='Username or email';
  selfService.append(element('p','An email address must be associated with the account. You may request a new code after one minute.'),recovery);
});
const contactForm=editor(['email','currentPassword'],{},async body=>{
  await mutate('/api/auth/contact',body,'POST');
  status.textContent='Recovery email saved.';
  contactForm.reset();
});
contactForm.elements.email.type='email';
contactForm.elements.currentPassword.type='password';
contactForm.prepend(element('h3','Recovery email'),element('p','Associate an email with your account to receive password-reset codes.'));
document.querySelector('#settings').append(contactForm);
