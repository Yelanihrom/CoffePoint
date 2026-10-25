const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../..');
const source = path.join(root, 'app/src/main/java/ru/coffeepoint');

function kotlinFiles(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const file = path.join(dir, entry.name);
    return entry.isDirectory() ? kotlinFiles(file) : entry.name.endsWith('.kt') ? [file] : [];
  });
}

test('screens use the shared dialog kit instead of independent dialog layouts', () => {
  const offenders = kotlinFiles(source).filter(file => {
    if (file === path.join(source, 'shared/component/AppDialogs.kt')) return false;
    return /\b(?:AlertDialog|BasicAlertDialog|DatePickerDialog|TimePickerDialog)\s*\(/.test(fs.readFileSync(file, 'utf8'));
  });
  assert.deepEqual(offenders.map(file => path.relative(root, file)), []);
});

test('modal design rules are documented for future changes', () => {
  const doc = fs.readFileSync(path.join(root, 'docs/ui-kit.md'), 'utf8');
  for (const component of ['AppDialog', 'AppDialogButton', 'AppDialogActions', 'AppDatePickerDialog', 'AppTimePickerDialog']) {
    assert.ok(doc.includes(component), `Missing UI-kit rule: ${component}`);
  }
});
