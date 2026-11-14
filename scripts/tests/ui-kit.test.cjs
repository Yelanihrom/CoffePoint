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

test('settings and analytics reuse the shared submenu row', () => {
  for (const relative of ['feature/settings/ui/SettingsScreen.kt', 'feature/analytics/ui/AnalyticsMenuScreen.kt']) {
    const text = fs.readFileSync(path.join(source, relative), 'utf8');
    assert.match(text, /import ru\.coffeepoint\.shared\.component\.AppMenuItem/);
    assert.match(text, /AppMenuItem\(/);
  }
  assert.ok(fs.readFileSync(path.join(root, 'docs/ui-kit.md'), 'utf8').includes('AppMenuItem'));
});

test('earnings and analytics reuse the shared period selector', () => {
  for (const relative of [
    'feature/earnings/ui/MonthlyEarningsScreen.kt',
    'feature/analytics/ui/SalesAnalyticsScreen.kt',
  ]) {
    const text = fs.readFileSync(path.join(source, relative), 'utf8');
    assert.match(text, /import ru\.coffeepoint\.shared\.component\.AppPeriodSelector/);
    assert.match(text, /\bAppPeriodSelector\s*\(/);
    assert.doesNotMatch(text, /fun\s+(?:PeriodDateCard|PeriodDate)\s*\(/);
  }
  const doc = fs.readFileSync(path.join(root, 'docs/ui-kit.md'), 'utf8');
  for (const component of ['AppPeriodSelector', 'AppPeriodDateField']) {
    assert.ok(doc.includes(component), `Missing UI-kit rule: ${component}`);
  }
  const instructions = fs.readFileSync(path.join(root, 'AGENTS.md'), 'utf8');
  assert.ok(instructions.includes('AppPeriodSelector'));
});

test('foreground data operations reuse the blocking loader', () => {
  for (const relative of [
    'feature/analytics/ui/SalesAnalyticsScreen.kt',
    'feature/analytics/ui/ServiceVisitHistoryScreen.kt',
    'feature/earnings/ui/MonthlyEarningsScreen.kt',
    'feature/settings/ui/NotificationSettingsScreen.kt',
    'feature/supplycatalog/ui/SupplyCatalogScreen.kt',
  ]) {
    const text = fs.readFileSync(path.join(source, relative), 'utf8');
    assert.match(text, /import ru\.coffeepoint\.shared\.component\.SyncSendingOverlay/);
    assert.match(text, /\bSyncSendingOverlay\s*\(/);
    assert.doesNotMatch(text, /\b(?:CircularProgressIndicator|LinearProgressIndicator|BlockingLoaderOverlay)\s*\(/);
  }
});
