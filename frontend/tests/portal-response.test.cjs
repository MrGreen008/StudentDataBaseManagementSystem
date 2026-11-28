const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../dist/portal.js'), 'utf8');
const requestSource = source.slice(source.indexOf('async function request('), source.indexOf('async function loadAccount('));

async function call(status, content) {
  const context = vm.createContext({
    fetch: async () => new Response(content, { status }),
    showSignIn() {}
  });
  vm.runInContext(requestSource, context);
  return context.request('/api/auth/register', { method: 'POST' });
}

test('registration accepts an empty Created response', async () => {
  assert.equal(await call(201, null), null);
});

test('successful responses preserve JSON and support No Content', async () => {
  assert.equal((await call(200, '{"message":"Saved"}')).message, 'Saved');
  assert.equal(await call(204, null), null);
});

test('malformed success payloads produce a readable message', async () => {
  await assert.rejects(call(200, '{"message":'), /unreadable response/);
});

test('failed responses preserve the server explanation', async () => {
  await assert.rejects(call(503, '{"detail":"Email service unavailable"}'), /Email service unavailable/);
});
