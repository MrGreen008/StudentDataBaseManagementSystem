# Vishvaya Public Website

Release 1 public website for the fictional Vishvaya Institute of Technology demo. It uses plain HTML, CSS, and JavaScript and needs no package install.

Open `dist/index.html` in a browser. Bootstrap and optional web fonts load from external CDNs. All branch offerings, honors/minors, announcements, metrics, credentials, testimonials, and contacts are demo placeholders; verify them before publishing. Student and faculty bulletins belong in authenticated portals, not on this public page.

## Design notes

Run `node --test frontend/tests/portal-response.test.cjs` to check portal response handling, including successful registration responses with empty bodies. The portal accepts an empty HTTP 201 response without displaying a JSON parsing error.

The authenticated R2 portal uses `dist/portal.html`, `portal.js`, `portal-workspace.js` and `portal.css`, served through Spring Boot. Start the backend to use sign-in, searchable tables, reference selectors and targeted notices. The role-specific login and dashboard photography reuses the existing generated demo images. Portal notice templates are unpublished examples until an authorized publisher verifies the details.

- Includes nine generated, optimized concept images under `dist/images`.
- Bootstrap 5.3.9 CSS and JavaScript are loaded from the official jsDelivr CDN; an internet connection is needed for Bootstrap and the optional web fonts.
- The 12-card department carousel supports arrow controls, horizontal scrolling, and vertical mouse-wheel scrolling while hovered. Selecting a department opens proposed honours and minors.
- Scroll reveals, hover lift, dropdown navigation, the admission dialog, and the responsive layout are interactive.
- Reference inspiration: YCCE, IIT Bombay/Delhi, IIM Ahmedabad/Bangalore, and NMIMS. The page uses original layout and wording; all institution-specific content is fictional sample copy.
